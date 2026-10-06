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

	slog.Info("网关已退出")
}
