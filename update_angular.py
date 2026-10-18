import json
with open('d:/codesentinel-ai/codesentinel-ui/angular.json', 'r') as f:
    data = json.load(f)

# Add proxyConfig to serve options
serve = data['projects']['codesentinel-ui']['architect']['serve']
if 'options' not in serve:
    serve['options'] = {}
serve['options']['proxyConfig'] = 'proxy.conf.json'

with open('d:/codesentinel-ai/codesentinel-ui/angular.json', 'w') as f:
    json.dump(data, f, indent=2)
