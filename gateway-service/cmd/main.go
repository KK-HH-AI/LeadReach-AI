package main

import (
	"context"
	"log/slog"
	"os"
	"os/signal"
	"syscall"

	"github.com/KK-HH-AI/LeadReach-AI/gateway-service/internal/config"
	"github.com/KK-HH-AI/LeadReach-AI/gateway-service/internal/server"
)

// 【心智模型：网关的启动流程】
// 函数 main():
//	第 1 步：准备日志器（JSON 格式，方便部署到服务器后收集日志）；
//	第 2 步：加载配置（读 configs/config.yaml 和 routes.yaml）；
//		如果配置加载失败：打印错误并退出；
//	第 3 步：挂好"退出信号"（用户按 Ctrl+C / 系统发 SIGTERM）：
//		一旦收到信号，ctx 会被自动取消 → 触发 server 优雅停机；
//	第 4 步：组装零件并启动网关；
//		如果启动失败：打印错误并退出；
//	如果走到这里：网关已正常退出。
func main() {
	// ─── 第 1 步：准备日志器 ───
	logger := slog.New(slog.NewJSONHandler(os.Stdout, &slog.HandlerOptions{
		Level: slog.LevelInfo,
	}))
	slog.SetDefault(logger)

	// ─── 第 2 步：加载配置 ───
	cfg, err := config.Load()
	if err != nil {
		slog.Error("加载配置失败", "error", err)
		os.Exit(1)
	}

	// ─── 第 3 步：挂好退出信号 ───
	ctx, stop := signal.NotifyContext(context.Background(), os.Interrupt, syscall.SIGTERM)
	defer stop()

	// ─── 第 4 步：组装零件并启动 ───
	srv := server.New(cfg, logger)
	if err := srv.Start(ctx); err != nil {
		slog.Error("网关运行出错", "error", err)
		os.Exit(1)
	}

	slog.Info("网关已优雅退出")
}
