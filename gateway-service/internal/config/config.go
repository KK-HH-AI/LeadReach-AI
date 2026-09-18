// 配置包：把 configs/ 下的 yaml 文件读进 Go 结构体
//
// 【心智模型】
// 函数 Load():
//	创建"主配置阅读器"，读 config.yaml；
//	把 yaml 内容整个倒进 Config 结构体；
//	创建"路由配置阅读器"，读 routes.yaml；
//	把 routes 字段单独倒进 cfg.Routes；
//	返回组装好的 Config。

package config

import (
	"fmt"

	"github.com/spf13/viper"
)

// RouteConfig 一条转发规则，对应 routes.yaml 里的一条
type RouteConfig struct {
	ID          string   `mapstructure:"id"`          // 规则名，比如 auth-public
	Prefix      string   `mapstructure:"prefix"`      // 网关收到的路径前缀，比如 /api/v1/auth
	Target      string   `mapstructure:"target"`      // 转发到哪个服务，比如 user-service
	StripPrefix bool     `mapstructure:"stripPrefix"` // 是否剥掉前缀再转发（本项目先都是 false）
	Middlewares []string `mapstructure:"middlewares"` // 这条路由挂哪些中间件，按名字引用
}

// Config 网关的全部配置（就是 config.yaml + routes.yaml 合起来的样子）
type Config struct {
	// app: 服务自身信息
	App struct {
		Name string `mapstructure:"name"`
		Env  string `mapstructure:"env"`
		Port int    `mapstructure:"port"`
	} `mapstructure:"app"`

	// jwt: 校验登录令牌用的密钥（必须和 user-service 签发时一致）
	JWT struct {
		Secret string `mapstructure:"secret"`
		Header string `mapstructure:"header"`
		Prefix string `mapstructure:"prefix"`
	} `mapstructure:"jwt"`

	// redis: 目前网关暂未使用，先保留配置（未来可做限流/缓存）
	Redis struct {
		Addr     string `mapstructure:"addr"`
		Password string `mapstructure:"password"`
		DB       int    `mapstructure:"db"`
	} `mapstructure:"redis"`

	// services: 服务发现表，服务名 → 地址（本地开发阶段写死在这里）
	Services map[string]string `mapstructure:"services"`

	// routes: 转发规则列表
	Routes []RouteConfig
}

// Load 读取 configs/ 下的配置文件，组装成 Config 返回
func Load() (*Config, error) {
	cfg := &Config{}

	// ─── 第 1 步：读主配置 config.yaml ───
	// viper 是通用配置库，支持 yaml/json/env，这里只用到 yaml
	mainViper := viper.New()
	mainViper.SetConfigFile("configs/config.yaml")
	if err := mainViper.ReadInConfig(); err != nil {
		return nil, fmt.Errorf("读取 config.yaml 失败: %w", err)
	}
	// Unmarshal：把 yaml 内容按字段名（tag）倒进 cfg 结构体
	if err := mainViper.Unmarshal(cfg); err != nil {
		return nil, fmt.Errorf("解析 config.yaml 失败: %w", err)
	}

	// ─── 第 2 步：读路由配置 routes.yaml ───
	// 单独一个文件，以后加路由只改 routes.yaml，不动代码
	routeViper := viper.New()
	routeViper.SetConfigFile("configs/routes.yaml")
	if err := routeViper.ReadInConfig(); err != nil {
		return nil, fmt.Errorf("读取 routes.yaml 失败: %w", err)
	}
	// 只把 routes 这一整段倒进 cfg.Routes
	if err := routeViper.UnmarshalKey("routes", &cfg.Routes); err != nil {
		return nil, fmt.Errorf("解析 routes.yaml 失败: %w", err)
	}

	return cfg, nil
}
