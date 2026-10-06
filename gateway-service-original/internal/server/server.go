package server

import (
	"context"
	"errors"
	"fmt"
	"log/slog"
	"net/http"
	"time"

	"github.com/KK-HH-AI/LeadReach-AI/gateway-service/internal/config"
	"github.com/KK-HH-AI/LeadReach-AI/gateway-service/internal/discovery"
	"github.com/KK-HH-AI/LeadReach-AI/gateway-service/internal/middleware"
	"github.com/KK-HH-AI/LeadReach-AI/gateway-service/internal/proxy"
	"github.com/gin-gonic/gin"
)

// Server 网关本体
type Server struct {
	cfg    *config.Config
	logger *slog.Logger
	engine *gin.Engine // gin引擎，真正干活的HTTP处理器
}

// New 组装所有零件，返回一台"待启动"的网关
func New(cfg *config.Config, logger *slog.Logger) *Server {
	// ─── 第 1 步：服务发现零件 ───
	resolver := discovery.NewStaticResolver(cfg.Services)

	// ─── 第 2 步：中间件注册表零件（工厂模式的核心用法）───
	registry := middleware.NewRegistry()
	registry.Register("cors", middleware.CORS)
	registry.Register("requestid", middleware.RequestID)
	// auth 需要密钥参数，而注册表只认"无参工厂"，所以用闭包把参数绑死：
	// "将来谁要 auth，我就拿这份密钥现做一个给他"
	registry.Register("auth", func() gin.HandlerFunc {
		return middleware.JWTAuth(cfg.JWT.Secret, cfg.JWT.Header, cfg.JWT.Prefix)
	})

	// ─── 第 3 步：gin 引擎 ───
	engine := gin.New()
	// 关闭尾部斜杠自动重定向：否则 /api/v1/teams 会被 301 到 /api/v1/teams/，
	// 而下游 Spring 默认不匹配带尾斜杠的路径，导致 301 → 404 链路
	engine.RedirectTrailingSlash = false
	engine.Use(gin.Recovery()) // 兜底：handler 恐慌时返回 500，而不是让进程崩掉

	// /health 健康检查：不经过任何中间件，部署后可用 curl 探活
	engine.GET("/health", func(c *gin.Context) {
		c.JSON(http.StatusOK, gin.H{
			"status": "ok",
			"app":    cfg.App.Name,
			"env":    cfg.App.Env,
		})
	})

	// ─── 第 4 步：按配置逐条挂路由 ───
	for _, route := range cfg.Routes {
		// 4.1 按规则里的名字列表"点菜"，得到真实中间件链
		chain := registry.Build(route.Middlewares)

		// 4.2 为这条规则造一个反向代理
		p := proxy.New(resolver, route.Target, route.Prefix, route.StripPrefix)

		// 4.3 挂到前缀下：/api/v1/auth/* 之后的任何请求都进这条链
		//     同时注册精确前缀路由（无尾斜杠）：gin 的 "/*path" 通配只匹配带斜杠路径，
		//     不加 "" 这条，POST /api/v1/companies 会被直接 404
		group := engine.Group(route.Prefix, chain...)
		group.Any("", p.Handler())
		group.Any("/*path", p.Handler())

		logger.Info("路由已挂载",
			"id", route.ID,
			"prefix", route.Prefix,
			"target", route.Target,
			"middlewares", route.Middlewares,
		)
	}

	return &Server{cfg: cfg, logger: logger, engine: engine}
}

// Start 启动监听，并负责优雅停机
func (s *Server) Start(ctx context.Context) error {
	httpServer := &http.Server{
		Addr:    fmt.Sprintf(":%d", s.cfg.App.Port),
		Handler: s.engine,
	}

	// 错误通道：监听 goroutine 里的错误要能传回主流程
	errCh := make(chan error, 1)

	go func() {
		s.logger.Info("网关启动", "addr", httpServer.Addr)
		if err := httpServer.ListenAndServe(); err != nil && !errors.Is(err, http.ErrServerClosed) {
			errCh <- err
		}
	}()

	// 阻塞等待：信号 或 监听出错，谁先来听谁的
	select {
	case <-ctx.Done():
		s.logger.Info("收到退出信号，开始优雅停机")
	case err := <-errCh:
		return err
	}

	// 优雅停机：给正在处理的请求最多 5 秒收尾时间
	shutdownCtx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
	defer cancel()
	return httpServer.Shutdown(shutdownCtx)
}
