src = 'unstructured.qry'
dst = 'combine.qry'

weights1 = {'url':1, 'title':1, 'inlink':1, 'body':9, 'keywords':1}
norm1 = 0.0
for name, weight in weights1.items():
    if weight == 0.0:
        del weights1[name]
    else:
        norm1 += weight
for name, weight in weights1.items():
    weights1[name] = weight / norm1

print weights1

weights2 = [3, 5, 2]

norm2 = sum(weights2) * 1.0
for i in xrange(0, len(weights2)):
    weights2[i] /= norm2

print weights2

totalWeight = [9, 1]
norm = sum(totalWeight) * 1.0
for i in xrange(0, len(totalWeight)):
    totalWeight[i]/=norm
print "total weight", totalWeight


f = open(dst, 'w')

for line in open(src, 'r'):
    seg = line.strip().split(':')
    words = seg[1].split(' ')
    #buf = seg[0] + ':#AND('
    buf = "#AND("
    for word in words:
        q = '#WSUM('
        for name, weight in weights1.items():
            q += str(weight) + ' ' + word + '.' + name + ' '
        q += ')'
        buf += q + ' '
    buf += ')  '

    buf1 = str(totalWeight[0]) + " " +buf

    seg = line.strip().split(':')
    words = seg[1].split(' ')
    #buf = seg[0] + ':' + '#WAND('
    buf = '#WAND('
    if len(words) == 1:
        buf += str(weights2[1] + weights2[2]) + ' ' + words[0] + ' '
    if weights2[0] > 0.0:
        buf += str(weights2[0]) + ' '
        buf += '#AND('
        for word in words:
            buf += word + ' '
        buf += ') '
    if weights2[1] > 0.0 and len(words) > 1:
        buf += str(weights2[1]) + ' '
        buf += '#AND('
        for i in xrange(1, len(words)):
            buf += '#NEAR/1(' + words[i - 1] + ' ' + words[i] + ') '
        buf += ') '
    if weights2[2] > 0.0 and len(words) > 1:
        buf += str(weights2[2]) + ' '
        buf += '#AND('
        for i in xrange(1, len(words)):
            buf += '#WINDOW/8(' + words[i - 1] + ' ' + words[i] + ') '
        buf += ')'
    buf += ')'
    
    buf2 = str(totalWeight[1]) + " " +buf
    print "buf2", buf2

    buftotal = seg[0] +": #WAND(" +buf1 + buf2 +")\n"
    f.write(buftotal)



f.close()
