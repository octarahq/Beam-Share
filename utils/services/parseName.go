package utilsServices

import "strings"

func ParseName(name string) string {
	output := ""

	output = strings.ReplaceAll(name, "\\ ", " ")

	return output
}
