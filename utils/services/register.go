package utilsServices

import (
	"github.com/grandcat/zeroconf"
)

func RegisterDevice(instance string, service string, domain string, port int, text []string) (zeroconf.Server, error) {
	server, err := zeroconf.Register(instance, service, domain, port, text, nil)

	return *server, err
}
