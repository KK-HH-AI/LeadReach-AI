package discovery

// 定义接口
// 根据服务名拿到服务地址，并且支持资源关闭
type Resolver interface {
	// Resolve方法：根据服务名拿到服务http地址
	Resolve(serviceName string) string
	// Close方法：关闭Resolver持有的资源
	Close() error
}
