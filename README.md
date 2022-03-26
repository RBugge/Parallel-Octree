# Parallel Octree

## Build and Run

### Build

V1: `javac -d build ./ParallelOctree/*.java`\
V2: `javac -d build ./ParallelOctreeV2/*.java`

### Run

V1: `java -cp ./build ParallelOctree.Main`\
V2: `java -cp ./build ParallelOctreeV2.Main`

### Both

V1: `javac -d build ./ParallelOctree/*.java && java -cp ./build ParallelOctree.Main`\
V2: `javac -d build ./ParallelOctreeV2/*.java && java -cp ./build ParallelOctreeV2.Main`

### Optional Arguments

`-o` &ensp;Model file name (no ext.).\
&ensp;&ensp;&ensp;&ensp;Default: `cube`

`-n` &ensp;Minimum number of threads to test with.\
&ensp;&ensp;&ensp;&ensp;Default: `1`

`-m` &ensp;Maximum number of threads to test with.\
&ensp;&ensp;&ensp;&ensp;Default: `6`

`-l` &ensp;Vertex limit for octants\
&ensp;&ensp;&ensp;&ensp;Default: `10`

#### Example

`java -cp ./build ParallelOctree.Main -o sphere -n 6 -m 12 -l 5`

## Output

Output is generated for each version of the octree and at each number of threads in the range provided.

Terminal and Files:\
V1: `output.txt`\
V2: `V2insertOutput.txt` and `V2removeOutput.txt`

Results:

```Text
<Octree Version>
        Model: <model name>
        # Threads: <number of threads used>
        # Vertices: <number of vertices in model>
        Runtime: <time to run test in seconds>
...
...
...
```

## Notes

Models are currently loaded in from `.gum` files. A tool is provided to convert `.obj` to `.gum` to use models not included with the project. It is planned to load `.obj` files directly in the final version, however, it is not a top priority.
