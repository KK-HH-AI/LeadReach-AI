package middleware

import (
	"fmt"
	"net/http"
	"strings"

	"github.com/gin-gonic/gin"
	"github.com/golang-jwt/jwt/v5"
)

// JWTAuth 返回一个校验 JWT 的中间件。
//
// 参数说明：
//   - secret：签名密钥，必须和签发方（user-service）用的一致
//   - header：从哪个请求头取 Token，通常是 "Authorization"
//   - prefix：Token 前缀，通常是 "Bearer"，和 Token 之间用空格分隔
//
// 为什么用"函数工厂"而不是直接返回 gin.HandlerFunc？
//
//	中间件需要参数（secret 等），这些参数在启动时才知道。
//	JWTAuth(secret, ...) 返回一个"已绑定参数的 handler"，
//	gin 就能把它当普通中间件使用。
//
// 工作流程：
//  1. 从请求头取 Token 字符串（形如 "Bearer eyJxxx"）
//  2. 校验格式、切分出真正的 Token
//  3. 用 secret 验证签名、检查是否过期
//  4. 从 claims 中提取用户信息，注入到请求头供下游使用
//  5. 放行
//
// 中间件用 401 响应表示鉴权失败：
//   - 401 Unauthorized：没有 Token 或 Token 无效
//   - 403 Forbidden：Token 有效但没权限（这里暂不涉及）
func JWTAuth(secret, header, prefix string) gin.HandlerFunc {
	return func(c *gin.Context) {
		// ---------- 第 1 步：从请求头取出原始字符串 ----------
		raw := c.GetHeader(header)
		if raw == "" {
			// 请求头为空，直接拒绝
			// 用 AbortWithStatusJSON 一次性完成"中断 + 返回 JSON"
			c.AbortWithStatusJSON(http.StatusUnauthorized, gin.H{
				"code": 401,
				"msg":  "缺少 " + header + " 头",
			})
			// 注意：AbortWithStatusJSON 内部会调用 c.Abort()，
			// 中断后续中间件和 handler 的执行，所以这里不用再 return
			// 但为了代码清晰，实际写的时候还是习惯性 return
			return
		}

		// ---------- 第 2 步：切分 "Bearer xxx" ----------
		//
		// strings.SplitN(s, sep, n)：
		//   - 按 sep 切分 s
		//   - 最多切出 n 段
		//   - n=2 表示"以第一个空格为界，切成两段"
		//
		// 为什么用 SplitN 而不是 Split？
		//   JWT 本身可能包含 "." 但不包含空格，
		//   而 "Bearer xxx yyy" 这种异常输入用 SplitN 更安全。
		parts := strings.SplitN(raw, " ", 2)
		if len(parts) != 2 || parts[0] != prefix {
			c.AbortWithStatusJSON(http.StatusUnauthorized, gin.H{
				"code": 401,
				"msg":  header + " 格式错误，应为 \"" + prefix + " <token>\"",
			})
			return
		}

		tokenStr := parts[1]

		// ---------- 第 3 步：解析并校验 Token ----------
		//
		// jwt.Parse 的三个关键点：
		//   a) 会验证签名是否匹配
		//   b) 会检查 exp 过期时间（如果 claims 里有 exp）
		//   c) 会调用第二个参数里的 keyFunc 获取密钥
		//
		// 【安全要点】keyFunc 里必须检查签名算法
		// 攻击者可能把 alg 改成 "none"（不签名）来绕过验证，
		// 必须限定只能用 HMAC 系列算法（HS256/HS384/HS512）。
		token, err := jwt.Parse(tokenStr, func(t *jwt.Token) (interface{}, error) {
			// 检查签名方法是否是 HMAC
			// 用类型断言 t.Method.(*jwt.SigningMethodHMAC) 判断
			if _, ok := t.Method.(*jwt.SigningMethodHMAC); !ok {
				return nil, fmt.Errorf("unexpected signing method: %v", t.Header["alg"])
			}
			// 返回密钥（字节切片）
			return []byte(secret), nil
		})

		if err != nil || !token.Valid {
			c.AbortWithStatusJSON(http.StatusUnauthorized, gin.H{
				"code": 401,
				"msg":  "Token 无效或已过期",
			})
			return
		}

		// ---------- 第 4 步：提取用户信息，注入请求头 ----------
		//
		// JWT 的 payload 部分（claims）是 map，键值对形式。
		// 签发时 user-service 用的是 MapClaims（默认），
		// 所以这里也断言成 jwt.MapClaims。
		//
		// 【为什么要把信息注入到请求头？】
		// 下游 Java 服务就不需要再解析一次 Token 了。
		// 网关已经验证过 Token 合法，直接把用户信息透传过去，
		// Java 端只要读 X-User-Phone 头就能拿到用户身份。
		//
		// 这样做的好处：
		//   - 减少重复解析，性能更好
		//   - 下游服务的代码更简单
		//
		// 【安全注意】注入的 X-User-* 头必须"覆盖"外部传入的值。
		// 攻击者可能伪造请求头 X-User-Id: 1 直接访问下游服务，
		// 所以必须先 Del 再 Set：请求头 map 是多值的，只 Set 可能留下伪造的那一条。
		if claims, ok := token.Claims.(jwt.MapClaims); ok {
			setStringClaim(c, claims, "phone", "X-User-Phone")
			setStringClaim(c, claims, "uid", "X-User-Id")
			// role / tenantId：user-service 登录时写入，
			// 下游服务据此判断身份角色和租户（多租户表级隔离）
			setStringClaim(c, claims, "role", "X-User-Role")
			setStringClaim(c, claims, "tenantId", "X-Tenant-Id")
			// position / teamId：岗位与团队，crm-service 据此推导数据权限
			// （BOSS→ALL / SALES_MANAGER→TEAM / SALES_REP→SELF）
			setStringClaim(c, claims, "position", "X-Job-Code")
			setStringClaim(c, claims, "teamId", "X-Team-Id")
		}

		// ---------- 第 5 步：放行 ----------
		c.Next()
	}
}

// setStringClaim 把 JWT 里的一个 claim 透传成请求头，写之前先删同名头防伪造。
// claims 里的值必须是字符串：签发方（user-service）用 String.valueOf 保证。
func setStringClaim(c *gin.Context, claims jwt.MapClaims, claimKey, headerKey string) {
	c.Request.Header.Del(headerKey)
	if v, ok := claims[claimKey].(string); ok && v != "" {
		c.Request.Header.Set(headerKey, v)
	}
}
