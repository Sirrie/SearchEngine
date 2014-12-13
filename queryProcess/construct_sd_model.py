
src = 'unstructured.qry'
dst = 'structured_SDM.qry'

weights = [3, 5, 2]

norm = sum(weights) * 1.0
for i in xrange(0, len(weights)):
    weights[i] /= norm

print weights

f = open(dst, 'w')
for line in open(src, 'r'):
    seg = line.strip().split(':')
    words = seg[1].split(' ')
    buf = seg[0] + ':' + '#WAND('
    if len(words) == 1:
        buf += str(weights[1] + weights[2]) + ' ' + words[0] + ' '
    if weights[0] > 0.0:
        buf += str(weights[0]) + ' '
        buf += '#AND('
        for word in words:
            buf += word + ' '
        buf += ') '
    if weights[1] > 0.0 and len(words) > 1:
        buf += str(weights[1]) + ' '
        buf += '#AND('
        for i in xrange(1, len(words)):
            buf += '#NEAR/1(' + words[i - 1] + ' ' + words[i] + ') '
        buf += ') '
    if weights[2] > 0.0 and len(words) > 1:
        buf += str(weights[2]) + ' '
        buf += '#AND('
        for i in xrange(1, len(words)):
            buf += '#WINDOW/8(' + words[i - 1] + ' ' + words[i] + ') '
        buf += ')'
    buf += ')'
    f.write(buf + '\n')
f.close()
        
        
