package ParallelOctreeV2;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class Main {
    static BufferedWriter bwInsert;
    static BufferedWriter bwRemove;
    static BufferedWriter bwInsertRemove;
    static String model = "cube";
    static int minThreads = 1;
    static int maxThreads = 12;
    static int octantLimit = 10;

    public static void main(String[] args) throws IOException, InterruptedException {
        bwInsert = new BufferedWriter(new FileWriter("V2insertOutput.txt"));
        bwRemove = new BufferedWriter(new FileWriter("V2removeOutput.txt"));
        bwInsertRemove = new BufferedWriter(new FileWriter("V2insertRemoveOutput.txt"));

        System.out.println("Type\t\tTest\t#Threads\t\tRuntime\n");
        bwInsert.write("Type\tTest\t#Threads\tRuntime\n");
        bwRemove.write("Type\tTest\t#Threads\tRuntime\n");
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "-n":
                    minThreads = Integer.parseInt(args[i + 1]);
                    break;
                case "-m":
                    maxThreads = Integer.parseInt(args[i + 1]);
                    break;
                case "-l":
                    octantLimit = Integer.parseInt(args[i + 1]);
                    break;
                case "-o":
                    model = args[i + 1];
                    break;

                default:
                    break;
            }
        }
        // Load vertices and indices
        // BufferedReader br = new BufferedReader(new FileReader(args[0]));
        BufferedReader br = new BufferedReader(new FileReader("Models/" + model + ".gum"));
        br.readLine(); // Ignore name

        // Read/split line containing vertex/index count, third tokens are the numbers
        int numVerts = Integer.parseInt(br.readLine().split(" ")[2]);
        int numIndices = Integer.parseInt(br.readLine().split(" ")[2]);

        br.readLine(); // Ignore color count

        String vertexDataStr[] = br.readLine().split(" ");
        String indexDataStr[] = br.readLine().split(" ");

        br.close();

        Vertex vertexData[] = new Vertex[numVerts];
        for (int i = 0; i < numVerts; i++) {
            vertexData[i] = new Vertex(
                    Double.parseDouble(vertexDataStr[(i) * 3 + 1]),
                    Double.parseDouble(vertexDataStr[(i) * 3 + 2]),
                    Double.parseDouble(vertexDataStr[(i) * 3 + 3]));
        }

        int indexData[] = new int[numIndices];
        for (int i = 0; i < numIndices; i++) {
            indexData[i] = Integer.parseInt(indexDataStr[i + 1]);
        }

        // Compute bounds
        double halfSize = 0;
        for (Vertex vertex : vertexData) {
            for (int i = 0; i < 3; i++) {
                if (Math.abs(vertex.xyz[i]) > halfSize)
                    halfSize = Math.abs(vertex.xyz[i]);
            }
        }

        PointerOctree octree = new PointerOctree(octantLimit, halfSize);
        testOctree(octree, 1, vertexData);
        // testPointerOctree(model, vertexData);

        for (int i = minThreads; i <= maxThreads; i++) {
            // CoarseGrainOctree cgOctree = new CoarseGrainOctree(octantLimit, halfSize);
            // testOctree(cgOctree, i, vertexData);

            // FineGrainOctree fgOctree = new FineGrainOctree(octantLimit, halfSize);
            // testOctree(fgOctree, i, vertexData);

            // OptimisticOctree oOctree = new OptimisticOctree(octantLimit, halfSize);
            // testOctree(oOctree, i, vertexData);

            LockFreeOctree lfOctree = new LockFreeOctree(octantLimit, halfSize);
            testOctree(lfOctree, i, vertexData);
        }

        bwInsert.close();
        bwRemove.close();
        bwInsertRemove.close();
    }

    static void testOctree(Octree octree, int numThreads, Vertex vertexData[])
            throws InterruptedException, IOException {

        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CompletionService<Void> completionService = new ExecutorCompletionService<>(executor);
        int numVertices = vertexData.length;

        long start = System.nanoTime();

        // Test Insertion
        for (int i = 0; i < numThreads; i++) {
            final int id = i;
            completionService.submit(new Callable<Void>() {
                private final int ID = id;

                public Void call() {
                    for (int i = ID; i < numVertices; i += numThreads) {
                        octree.insert(vertexData[i]);
                    }
                    return null;
                }
            });
        }

        for (int i = 0; i < numThreads; i++) {
            completionService.take();
        }

        double execTime = (double) (System.nanoTime() - start) / Math.pow(10, 9);
        boolean verified = verify(octree, vertexData) == 0;
        printResults(bwInsert, "Insertion", octree.name, numThreads, vertexData.length, execTime, verified);

        // Test Removal
        for (int i = 0; i < numThreads; i++) {
            final int id = i;
            completionService.submit(new Callable<Void>() {
                private final int ID = id;

                public Void call() {
                    for (int i = ID; i < numVertices; i += numThreads) {
                        octree.remove(vertexData[i]);
                    }
                    return null;
                }
            });
        }

        for (int i = 0; i < numThreads; i++) {
            completionService.take();
        }
        executor.shutdown();

        execTime = (double) (System.nanoTime() - start) / Math.pow(10, 9);
        verified = verify(octree, vertexData) == numVertices;
        printResults(bwRemove, "Removal", octree.name, numThreads, vertexData.length, execTime, verified);
    }

    // TODO: Other error checking
    static int verify(Octree octree, Vertex vertexData[]) {
        int count = 0;
        for (var v : vertexData) {
            if (!octree.contains(v)) {
                count++;
            }
        }
        System.out.println("Missing: " + count);
        return count;
    }

    static void printResults(BufferedWriter file, String testType, String name, int numThreads, int numVertices,
            double runtime, boolean verified)
            throws IOException {
        String results = name + "\t" +
                            testType + "\t" +
                            // model + "\n" +
                            numThreads + "\t" +
                            // numVertices + "\n" +
                            // verified + "\n" +
                            runtime + "\n";
        System.out.print(results);
        file.write(results);
    }
}