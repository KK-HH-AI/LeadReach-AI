package middleware

import (
	"github.com/gin-gonic/gin"
	"github.com/google/uuid"
)

// HeaderRequestID 是请求 ID 用的 HTTP 头名字。
//
// 用常量而不是硬编码字符串，好处：
//   - 其他地方引用时不怕拼错
//   - 未来要改头名（比如改成 X-Trace-Id）只改这一处
//
// 命名用 "X-Request-Id" 是业界惯例，
// X- 前缀表示"自定义头"，不是 HTTP 标准头。
const HeaderRequestID = "X-Request-Id"

// RequestID 返回一个为每个请求注入唯一 ID 的中间件。
//
// 工作流程：
//  1. 检查请求头里有没有 X-Request-Id
//  2. 有就复用（上游服务传入的 ID，保证链路一致）
//  3. 没有就生成一个新的 UUID
//  4. 把 ID 分别写入三个地方：
//     - gin.Context 的 "requestId" key（供本服务内部使用）
//     - 请求头的 X-Request-Id（向下游服务透传）
//     - 响应头的 X-Request-Id（让调用方也能看到本次请求 ID）
//  5. 放行到下一个 handler
//
// 为什么用 UUID 而不是自增数字？
//   - 网关是多实例的，自增数字会冲突
//   - UUID 全局唯一，且无需中心化协调
//   - 学习阶段用 google/uuid 的 v4（随机）版本足够
//
// 为什么用函数工厂模式（返回 gin.HandlerFunc 而不是直接返回 handler）？
//   - 保持和其他中间件一致的风格
//   - 未来如果这个中间件需要参数（比如自定义头名），直接改参数即可
func RequestID() gin.HandlerFunc {
	// 返回的函数就是实际的 handler
	return func(c *gin.Context) {
		// 尝试从请求头读取 ID
		rid := c.GetHeader(HeaderRequestID)

		// 没有的话生成一个新的
		if rid == "" {
			rid = uuid.NewString()
		}

		// 存入 gin.Context，本服务的后续代码可以用 c.GetString("requestId") 取
		c.Set("requestId", rid)

		// 写入请求头，以便反向代理转发给下游服务时带上
		// 下游服务（比如 user-service）也能拿到同一个 ID，实现链路追踪
		c.Request.Header.Set(HeaderRequestID, rid)

		// 写入响应头，让客户端也能拿到 ID，方便报错时提供给后端排查
		c.Writer.Header().Set(HeaderRequestID, rid)

		// 交给下一个 handler（或下一个中间件）
		c.Next()
	}
}
