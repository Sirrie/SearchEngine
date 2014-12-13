
src = 'unstructured.qry'
dst = 'multirepresent.qry'

weights = {'url':9, 'title':0, 'inlink':0, 'body':1, 'keywords':0}

norm = 0.0
for name, weight in weights.items():
    if weight == 0.0:
        del weights[name]
    else:
        norm += weight
for name, weight in weights.items():
    weights[name] = weight / norm

f = open(dst, 'w')
for line in open(src, 'r'):
    seg = line.strip().split(':')
    words = seg[1].split(' ')
    buf = seg[0] + ':#AND('
    for word in words:
        q = '#WSUM('
        for name, weight in weights.items():
            q += str(weight) + ' ' + word + '.' + name + ' '
        q += ')'
        buf += q + ' '
    buf += ')\n'
    f.write(buf)
f.close()
