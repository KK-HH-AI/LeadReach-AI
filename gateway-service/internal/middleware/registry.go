//  中间件注册表，就是可以按照名字查找中间件

package middleware

import "github.com/gin-gonic/gin"

// 创建一个中间件注册表的结构体
type Registry struct {
	factories map[string]func() gin.HandlerFunc
}

// 新建一个注册表，初始化map
func NewRegistry() *Registry {
	return &Registry{
		factories: make(map[string]func() gin.HandlerFunc),
	}
}

// 注册中间件，把名字和生产中间件的函数存入map
func (r *Registry) Register(name string, factory func() gin.HandlerFunc) {
	r.factories[name] = factory
}

// 找到对应工厂 → 执行工厂函数 → 得到真实 gin 中间件，加入链。找不到名字直接跳过。
func (r *Registry) Build(names []string) []gin.HandlerFunc {
	chain := make([]gin.HandlerFunc, 0, len(names))
	for _, name := range names {
		if factory, ok := r.factories[name]; ok {
			chain = append(chain, factory())
		}
	}
	return chain
}
