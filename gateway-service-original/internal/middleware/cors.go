package middleware

import (
	"net/http"

	"github.com/gin-gonic/gin"
)

// CORS 返回一个处理跨域请求的中间件。
//
// 什么是 CORS？
//   - 浏览器安全机制：默认禁止 JS 请求不同源的接口
//   - 不同源 = 协议、域名、端口 三者任一不同
//   - 例如前端 http://localhost:5173 请求后端 http://localhost:8080
//     会被浏览器拦截，除非后端明确返回允许跨域的头
//
// 中间件的两个职责：
//  1. 对"预检请求"（OPTIONS）快速返回 204
//  2. 对正常请求添加 CORS 响应头
//
// 【预检请求是什么】
// 当浏览器发现请求是"非简单请求"（比如带 Authorization 头、
// 或者用 PUT/DELETE 方法），会先发一个 OPTIONS 请求问服务器：
// "我接下来要发一个带 Authorization 的 POST，你允许吗？"
// 服务器必须正确回应，浏览器才会发真正的请求。
//
// 【当前实现的选择】
// 学习阶段用 "*" 允许所有来源，简单方便。
// 生产环境必须改成白名单：
//   - 明确列出允许的前端域名，比如 "https://app.example.com"
//   - 并且要加 Allow-Credentials 的时候，Origin 不能用 "*"
func CORS() gin.HandlerFunc {
	return func(c *gin.Context) {
		// 允许任何来源访问（学习阶段）
		c.Writer.Header().Set("Access-Control-Allow-Origin", "*")

		// 允许的 HTTP 方法
		c.Writer.Header().Set("Access-Control-Allow-Methods",
			"GET,POST,PUT,DELETE,OPTIONS")

		// 允许客户端携带的头
		// 这里必须包含 Authorization（JWT）和 X-Request-Id（链路追踪）
		// 否则浏览器会在预检阶段就把请求拦下来
		c.Writer.Header().Set("Access-Control-Allow-Headers",
			"Content-Type,Authorization,X-Request-Id")

		// 告诉浏览器：预检结果缓存 24 小时
		// 避免每个请求都先发一次 OPTIONS
		// 单位是秒：86400 = 24 * 60 * 60
		c.Writer.Header().Set("Access-Control-Max-Age", "86400")

		// 如果是预检请求，直接返回 204 No Content，不再往下走
		// gin 里用 AbortWithStatus 来中断中间件链
		if c.Request.Method == http.MethodOptions {
			c.AbortWithStatus(http.StatusNoContent)
			return
		}

		// 普通请求：放行
		c.Next()
	}
}
