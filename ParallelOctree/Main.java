package ParallelOctree;

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
import java.util.Random;

class Main {
    static BufferedWriter bwInsert;
    static BufferedWriter bwRemove;
    static BufferedWriter bwInsertRemove;
    static String model = "cube";
    static int minThreads = 1;
    static int maxThreads = 12;
    static int octantLimit = 10;

    public static void main(String[] args) throws IOException, InterruptedException {
        bwInsert = new BufferedWriter(new FileWriter("V1insertOutput.txt"));
        bwRemove = new BufferedWriter(new FileWriter("V1removeOutput.txt"));
        bwInsertRemove = new BufferedWriter(new FileWriter("V1insertRemoveOutput.txt"));

        System.out.println("Type\t\tTest\t#Threads\t\tRuntime\n");
        bwInsert.write("Type\tTest\t#Threads\tRuntime\n");
        bwRemove.write("Type\tTest\t#Threads\tRuntime\n");
        bwInsertRemove.write("Type\tTest\t#Threads\tRuntime\n");
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

        PointerOctree octree = new PointerOctree(octantLimit);
        testOctree(octree, 1, vertexData);
        // testPointerOctree(model, vertexData);

        for (int i = minThreads; i <= maxThreads; i++) {
            // CoarseGrainOctree cgOctree = new CoarseGrainOctree(octantLimit);
            // testOctree(cgOctree, i, vertexData);

            FineGrainOctree fgOctree = new FineGrainOctree(octantLimit);
            testOctree(fgOctree, i, vertexData);

            OptimisticOctree oOctree = new OptimisticOctree(octantLimit);
            testOctree(oOctree, i, vertexData);
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

        execTime = (double) (System.nanoTime() - start) / Math.pow(10, 9);
        verified = verify(octree, vertexData) == numVertices;
        printResults(bwRemove, "Removal", octree.name, numThreads, vertexData.length, execTime, verified);

        // Test Insertion/Removal
        Random rand = new Random();
        for (int i = 0; i < numThreads; i++) {
            final int id = i;
            completionService.submit(new Callable<Void>() {
                private final int ID = id;
                int numVerticesInsert = ID;
                int numVerticesRemove = ID;
                int action;
                int[] done = new int[numVertices];
                public Void call() {
                    while (numVerticesInsert < numVertices || numVerticesRemove < numVertices)
                    {
                        // Randomly get action.
                        action = rand.nextInt(2);
                        // If action is set to 0, add the next vertex if it's less than total vertices.
                        if (action == 0 && numVerticesInsert < numVertices)
                        {
                            done[numVerticesInsert] = 1;
                            octree.insert(vertexData[numVerticesInsert]);
                            numVerticesInsert += numThreads;
                        }
                        // If action is set to 1, remove the vertex from octree if it's already been added in.
                        else if (action == 1 && done[numVerticesRemove] == 1)
                        {
                            octree.remove(vertexData[numVerticesRemove]);
                            numVerticesRemove += numThreads;
                        }
                    }
                    return null;
                }
            });
        }

        executor.shutdown();

        for (int i = 0; i < numThreads; i++) {
            completionService.take();
        }

        execTime = (double) (System.nanoTime() - start) / Math.pow(10, 9);
        verified = verify(octree, vertexData) == numVertices;
        printResults(bwInsertRemove, "Insertion/Removal", octree.name, numThreads, vertexData.length, execTime, verified);
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
