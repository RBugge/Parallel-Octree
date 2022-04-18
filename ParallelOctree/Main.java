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

class Main {
    static BufferedWriter bw;
    static String model = "cube";
    static int minThreads = 1;
    static int maxThreads = 12;
    static int octantLimit = 10;

    public static void main(String[] args) throws IOException, InterruptedException {
        bw = new BufferedWriter(new FileWriter("output.txt"));
        System.out.println("Type\t\t#Threads\t\tRuntime\n");
        bw.write("Type\t#Threads\tRuntime\n");
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

        PointerOctree octree = new PointerOctree(octantLimit);
        testOctree(octree, 1, vertexData);
        // testPointerOctree(model, vertexData);

        for (int i = minThreads; i <= maxThreads; i++) {
            // CoarseGrainOctree cgOctree = new CoarseGrainOctree(octantLimit);
            // testOctree(cgOctree, i, vertexData);

            // // Working I think
            // FineGrainOctree fgOctree = new FineGrainOctree(octantLimit);
            // testOctree(fgOctree, i, vertexData);

            OptimisticOctree oOctree = new OptimisticOctree(octantLimit);
            testOctree(oOctree, i, vertexData);
        }

        bw.close();
    }

    static void testOctree(Octree octree, int numThreads, Vertex vertexData[])
            throws InterruptedException, IOException {
        long start = System.nanoTime();

        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CompletionService<Void> completionService = new ExecutorCompletionService<>(executor);
        int numVertices = vertexData.length;

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
        executor.shutdown();

        double execTime = (double) (System.nanoTime() - start) / Math.pow(10, 9);
        boolean verified = verify(octree, vertexData);
        printResults(octree.name, numThreads, vertexData.length, execTime, verified);
    }

    // TODO: Other error checking
    static boolean verify(Octree octree, Vertex vertexData[]) {
        boolean verified = true;
        int count = 0;
        for (var v : vertexData) {
            if (!octree.contains(v)) {
                // System.err.println("Error: Vertex " + v + " not found");
                verified = false;
                count++;
            }
        }
        System.out.println("Missing: " + count);
        return verified;
    }

    static void printResults(String name, int numThreads, int numVertices, double runtime, boolean verified)
            throws IOException {
        String results = name + "\t" +
                // model + "\t" +
                numThreads + "\t" +
                // numVertices + "\t" +
                // verified + "\t" +
                runtime + "\n";
        System.out.print(results);
        bw.write(results);
    }
}