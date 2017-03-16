# othello_vs

javac 1.8.0_121 

## input


    T
    X _
    b00 b01 ... b07
    b10 b11 ... b17
    ...
    b70 b71 ... b77


T: leftTime(ms)

X: turn(0:white, 1:black)

_: always -1

bij: stone at (i,j)

('.':empty, 'o':white, 'x':black)

## output

    i j

i,j: put stone at (i,j)

0 <= i,j <= 7

# Use "Flush"!

pythonをAIとして動かすとき、flushが必要になることがあります。

    sys.stdout.write("1 2\n")
    sys.stdout.flush()

    print(1,2, flush=True)
    
