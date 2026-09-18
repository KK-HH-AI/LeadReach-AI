// 服务发现接口的具体实现
package discovery

// 定义一个结构体
type StaticResolver struct {
	// 数据结构
	services map[string]string
}

func NewStaticResolver(m map[string]string) *StaticResolver {
	return &StaticResolver{services: m}
}

func (s *StaticResolver) Resolve(name string) string {
	return s.services[name]
}

func (s *StaticResolver) Close() error { return nil }
