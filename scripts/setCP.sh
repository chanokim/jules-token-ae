
cd ../target
CLASSPATH=`for i in *.jar; do echo -n "../target/$i:";done;`
cd ../lib
CLASSPATH="$CLASSPATH"`for i in *.jar; do echo -n "../lib/$i:";done;`
CLASSPATH="$CLASSPATH."

export CLASSPATH

cd ../scripts

