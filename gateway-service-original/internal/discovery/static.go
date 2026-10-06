// 服务发现接口的具体实现
// 服务发现，意思就是【服务名 -> 服务地址】映射表，根据服务名字查询对应的 IP 地址
// 没有注册中心，数据写在内存的map里
package discovery

// 定义一个结构体
type StaticResolver struct {
	// 数据结构，结构体成员
	services map[string]string
}

// 构造函数，初始化一个结构体
func NewStaticResolver(m map[string]string) *StaticResolver {
	return &StaticResolver{services: m}
}

// 使用key访问结构体成为的value的方法，根据服务名查询ip
func (s *StaticResolver) Resolve(name string) string {
	return s.services[name]
}

// 根据服务名关闭目标服务
func (s *StaticResolver) Close() error { return nil }
