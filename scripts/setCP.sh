
cd ../target
CLASSPATH=`for i in *.jar; do echo -n "../target/$i:";done;`
cd dependency
CLASSPATH="$CLASSPATH"`for i in *.jar; do echo -n "../target/dependency/$i:";done;`
CLASSPATH="$CLASSPATH."

export CLASSPATH

cd ../../scripts

