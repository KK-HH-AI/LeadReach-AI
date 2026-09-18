// 代理包：负责"替客户端把请求转发给下游服务"
//
// 【心智模型】
// 函数 Handler():
//	构造一个标准库的反向代理，并告诉它"怎么改目标地址"：
//		每次转发前：
//			用服务名查服务发现表，得到真实地址；
//			把请求的 scheme 和 host 改写成目标地址；
//			如果配置了剥前缀，就把路径里的前缀去掉；
//	返回一个 gin 处理器：
//		先确认目标服务存在，查不到直接回 502；
//		存在就交给反向代理去转发，并把响应原样回给客户端。

package proxy

import (
	"net/http"
	"net/http/httputil"
	"net/url"
	"strings"

	"github.com/KK-HH-AI/LeadReach-AI/gateway-service/internal/discovery"
	"github.com/gin-gonic/gin"
)

// Proxy 一条"转发规则"对应的代理
type Proxy struct {
	resolver    discovery.Resolver // 服务发现：服务名 → 地址
	target      string             // 目标服务名，比如 user-service
	prefix      string             // 这条路由挂在哪个前缀下，比如 /api/v1/auth
	stripPrefix bool               // 是否把前缀剥掉再转发
}

// New 创建代理
func New(resolver discovery.Resolver, target, prefix string, stripPrefix bool) *Proxy {
	return &Proxy{
		resolver:    resolver,
		target:      target,
		prefix:      prefix,
		stripPrefix: stripPrefix,
	}
}

// Handler 返回真正处理"转发"的 gin 处理器
func (p *Proxy) Handler() gin.HandlerFunc {
	// 标准库自带反向代理：它负责"把响应流式回写、错误处理"等脏活累活，
	// 我们只需要告诉它"每次转发前怎么改写请求地址"（Director）。
	rp := &httputil.ReverseProxy{
		Director: func(req *http.Request) {
			// 1. 服务发现：用服务名换真实地址
			raw := p.resolver.Resolve(p.target)
			target, _ := url.Parse(raw)

			// 2. 改写目标：让请求发往 http://<服务地址><原路径>
			req.URL.Scheme = target.Scheme
			req.URL.Host = target.Host
			req.Host = target.Host // Host 头也要改，否则下游可能不认

			// 3. 可选：剥掉前缀（本项目的路由都配了 stripPrefix=false，保留原路径）
			if p.stripPrefix {
				req.URL.Path = strings.TrimPrefix(req.URL.Path, p.prefix)
				req.URL.RawPath = strings.TrimPrefix(req.URL.RawPath, p.prefix)
			}
		},
	}

	return func(c *gin.Context) {
		// 先查一下目标服务在不在服务发现表里，不在直接 502
		if p.resolver.Resolve(p.target) == "" {
			c.AbortWithStatusJSON(http.StatusBadGateway, gin.H{
				"code": 502,
				"msg":  "未知服务: " + p.target,
			})
			return
		}
		// 转发：把客户端请求交给反向代理，响应直接写回客户端
		rp.ServeHTTP(c.Writer, c.Request)
	}
}
