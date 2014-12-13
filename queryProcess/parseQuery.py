import fileinput
writeFile = open ('multirepresent.qry', 'w')
weight = [0.05, 0.1, 0.05, 0.75, 0.05]
field  = ["url","keywords","title","body","inlink"]
for line in fileinput.input(['unstructured.qry']):
    qeryId =line.strip()
    print qeryId
    qeryId = qeryId.split(":")[0]
    words = line.split(":")[1].split(" ")
    print words
    length = len(words)
    words.pop()
    rLine = qeryId + ":"
    for word in words:
        tempLine = "#SUM("
        for i in xrange(len(field)):
            tempLine +=" " +str(weight[i])+" " +word.strip()+"."+ field[i]
        tempLine += ")"
        rLine+=tempLine + " "
    rLine+= "\n"
    writeFile.write(rLine)

writeFile.close()


