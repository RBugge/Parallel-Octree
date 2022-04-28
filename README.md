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
V1: `V1insertOutput.txt` and `V1removeOutput.txt` and `V1insertRemoveOutput.txt`\
V2: `V2insertOutput.txt` and `V2removeOutput.txt` and `V2insertRemoveOutput.txt`

Results:

```Text
Missing: <# Missing Vertices>
<Octree Version> <Test Type> <# Threads> <Runtime (s)>
...
...
...
```

## Notes

The number of missing vertices corresponds to the model used and the test being performed. Insertion should have `0` missing vertices, while the other two tests should be missing all vertices that are in the model.

Models are currently loaded in from `.gum` files. A tool is provided to convert `.obj` to `.gum` to use models not included with the project. It is planned to load `.obj` files directly in the final version, however, it is not a top priority.
