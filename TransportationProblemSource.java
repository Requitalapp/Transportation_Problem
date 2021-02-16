import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toCollection;

public class TransportationProblem {

    private static int[] demand;
    private static int[] supply;
    private static int[][] costs;
    private static Shipment[][] matrix;

    private static class Shipment {
        final double costPerUnit;
        final int r, c;
        double quantity;

        public Shipment(double q, double cpu, int r, int c) {
            quantity = q;
            costPerUnit = cpu;
            this.r = r;
            this.c = c;
        }
    }

    static void init(String filename) throws Exception {

        try (Scanner sc = new Scanner(new File(filename))) {
            int numSources = sc.nextInt();
            int numDestinations = sc.nextInt();

            List<Integer> src = new ArrayList<>();
            List<Integer> dst = new ArrayList<>();

            for (int i = 0; i < numSources; i++)
                src.add(sc.nextInt());

            for (int i = 0; i < numDestinations; i++)
                dst.add(sc.nextInt());

            // fix imbalance
            int totalSrc = src.stream().mapToInt(i -> i).sum();
            int totalDst = dst.stream().mapToInt(i -> i).sum();
            if (totalSrc > totalDst)
                dst.add(totalSrc - totalDst);
            else if (totalDst > totalSrc)
                src.add(totalDst - totalSrc);

            //System.out.println(totalDst + " " + totalSrc);

            supply = src.stream().mapToInt(i -> i).toArray();
            demand = dst.stream().mapToInt(i -> i).toArray();

            costs = new int[supply.length][demand.length];
            matrix = new Shipment[supply.length][demand.length];

            for (int i = 0; i < numSources + 1; i++)
                for (int j = 0; j < numDestinations; j++)
                    costs[i][j] = sc.nextInt();
        }
    }

    static void northWestCornerRule() {
        printNWResult();
        for (int r = 0, northwest = 0; r < supply.length; r++) {
            for (int c = northwest; c < demand.length; c++) {

                int quantity = Math.min(supply[r], demand[c]);
                if (quantity > 0) {
                    matrix[r][c] = new Shipment(quantity, costs[r][c], r, c);
                    System.out.println("Выбираем северо-западный элемент с координатами: " + (r+1) + "," + (c+1));
                    System.out.println("Минимум из a" + (r+1) + " = " + supply[r] + " и b" + (c+1) + " = " + demand[c] + " равен " + quantity);
                    System.out.println("Установим это значение в клетку");
                    supply[r] -= quantity;
                    demand[c] -= quantity;
                    System.out.println("Вычтем минимум из a" + (r+1) + " и b" + (c+1));
                    System.out.println("Новые значения: a" + (r+1) + " = " + supply[r] + " b" + (c+1) + " = " + demand[c]);
                    if (supply[r] == 0){
                        System.out.println("Установим прочерки в строке a" + (r+1));
                    } else{
                        System.out.println("Установим прочерки в столбце b" + (c+1));
                    }

                    printNWResult();

                    if (supply[r] == 0) {
                        northwest = c;
                        break;
                    }
                }
            }
            //printNWResult();
        }
    }

    static void modifiedDistribution(){
        Integer[] u = new Integer[supply.length];
        Integer[] v = new Integer[demand.length];
        u[0] = 0;

        for (int r = 0; r < supply.length; r++){
            for (int c = 0; c < demand.length; c++){
                Shipment s = matrix[r][c];
                if (s != null){
                    if (u[r] == null){
                        u[r] = costs[r][c] - v[c];
                    }else {
                        v[c] = costs[r][c] - u[r];
                    }
                }

            }
        }
        for (int r = 0; r < supply.length; r++){
            System.out.print(u[r] + " ");
        }

        for (int c = 0; c < demand.length; c++){
            System.out.print(v[c] + " ");
        }

    }

    static void steppingStone() {
        double maxReduction = 0;
        Shipment[] move = null;
        Shipment leaving = null;

        fixDegenerateCase();
        printResult();
        System.out.println("Вычислим оценки для всех не базисных клеток");
        for (int r = 0; r < supply.length; r++) {
            for (int c = 0; c < demand.length; c++) {

                if (matrix[r][c] != null)
                    continue;

                Shipment trial = new Shipment(0, costs[r][c], r, c);
                Shipment[] path = getClosedPath(trial);

                double reduction = 0;
                double lowestQuantity = Integer.MAX_VALUE;
                Shipment leavingCandidate = null;

                boolean plus = true;
                System.out.print("a" + (r+1) + "b" + (c+1) + " = ");
                for (Shipment s : path) {
                    if (plus) {
                        System.out.print(" + " + "(a" + (s.r+1) + "b" + (s.c+1) + ")" + s.costPerUnit);
                        reduction += s.costPerUnit;
                    } else {
                        reduction -= s.costPerUnit;
                        System.out.print(" - " + "(a" + (s.r+1) + "b" + (s.c+1) + ")" + s.costPerUnit);
                        if (s.quantity < lowestQuantity) {
                            leavingCandidate = s;
                            lowestQuantity = s.quantity;
                        }
                    }
                    plus = !plus;
                }
                System.out.println(" = " + reduction);
                //System.out.println("a" + r + "b" + c + " = " + reduction);
                if (reduction < maxReduction) {
                    move = path;
                    leaving = leavingCandidate;
                    maxReduction = reduction;
                }
            }
        }
        if (move != null) {
            System.out.println("Выберем клетку с наименьшей отрицательной оценкой: a" + (move[0].r + 1) + "b" + (move[0].c + 1) + " со значением " + maxReduction);
            System.out.print("Удалим базисные клетки со знаком ' - ' c наименьшим значением : ");
            double q = leaving.quantity;
            boolean plus = true;
            for (Shipment s : move) {
                s.quantity += plus ? q : -q;
                if (s.quantity == 0) {
                    matrix[s.r][s.c] = null;
                    System.out.print("a" + (s.r + 1) + "b" + (s.c + 1) + " ");
                } else {
                    matrix[s.r][s.c] = s;
                }
                //matrix[s.r][s.c] = s.quantity == 0 ? null : s;
                plus = !plus;
            }
            System.out.print("со значением " + q);
            System.out.println();
            System.out.println();
            steppingStone();
        } else{
            System.out.println("Отрицательных оценок нет, оптимальный план найден");
            printResult();
        }
    }

    static LinkedList<Shipment> matrixToList() {
        return stream(matrix)
                .flatMap(row -> stream(row))
                .filter(s -> s != null)
                .collect(toCollection(LinkedList::new));
    }

    static Shipment[] getClosedPath(Shipment s) {
        LinkedList<Shipment> path = matrixToList();
        path.addFirst(s);

        // remove (and keep removing) elements that do not have a
        // vertical AND horizontal neighbor
        while (path.removeIf(e -> {
            Shipment[] nbrs = getNeighbors(e, path);
            return nbrs[0] == null || nbrs[1] == null;
        }));

        // place the remaining elements in the correct plus-minus order
        Shipment[] stones = path.toArray(new Shipment[path.size()]);
        Shipment prev = s;
        for (int i = 0; i < stones.length; i++) {
            stones[i] = prev;
            prev = getNeighbors(prev, path)[i % 2];
        }
        return stones;

    }

    static Shipment[] getNeighbors(Shipment s, LinkedList<Shipment> lst) {
        Shipment[] nbrs = new Shipment[2];
        for (Shipment o : lst) {
            if (o != s) {
                if (o.r == s.r && nbrs[0] == null)
                    nbrs[0] = o;
                else if (o.c == s.c && nbrs[1] == null)
                    nbrs[1] = o;
                if (nbrs[0] != null && nbrs[1] != null)
                    break;
            }
        }
        return nbrs;
    }

    static void fixDegenerateCase() {
        final double eps = Double.MIN_VALUE;
        System.out.println("Проверим, является ли план вырожденным");
        System.out.println("X[Xij] = " + matrixToList().size() + " n = " + supply.length + " m = " + demand.length);
        if (supply.length + demand.length - 1 != matrixToList().size()) {
            System.out.println(matrixToList().size() + " != " + supply.length + " + " + demand.length + " -1");
            System.out.println("Значит план вырожденный");
            System.out.println("Найдем не базисную клетку из которой нельзя построить замкнутый цикл и поставим в нее 0");
            for (int r = 0; r < supply.length; r++)
                for (int c = 0; c < demand.length; c++) {
                    if (matrix[r][c] == null) {
                        Shipment dummy = new Shipment(eps, costs[r][c], r, c);
                        if (getClosedPath(dummy).length == 0) {
                            matrix[r][c] = dummy;
                            fixDegenerateCase();
                            return;
                        }
                    }
                }
        } else{
            System.out.println(matrixToList().size() + " = " + supply.length + " + " + demand.length + " -1");
            System.out.println("Значит план невырожденный");
        }
    }

    static void printResult() {
        //String filename
        //System.out.printf("Optimal solution %s%n%n", filename);
        double totalCosts = 0;

        for (int r = 0; r < supply.length; r++) {
            for (int c = 0; c < demand.length; c++) {

                Shipment s = matrix[r][c];
                if (s != null && s.r == r && s.c == c) {
                    System.out.printf("%3s/%s", (int) s.quantity, (int) s.costPerUnit);
                    totalCosts += (s.quantity * s.costPerUnit);
                } else
                    System.out.printf("%5s", ("-/" + costs[r][c]) );
            }
            System.out.println();
        }
        System.out.printf("%n Затраты: %s%n%n", totalCosts);
    }


    static void printNWResult() {
        for (int r = 0; r <= supply.length; r++) {
            for (int c = 0; c <= demand.length; c++) {
                if (c == demand.length && r==supply.length){
                    break;
                }
                if (c == demand.length){
                    System.out.printf(" %3s ", supply[r]);
                    continue;
                }

                if (r == supply.length){
                    System.out.printf(" %3s ", demand[c]);
                    continue;
                }

                Shipment s = matrix[r][c];

                if (s != null) {
                    System.out.printf(" %3s ", (int) s.quantity);
                } else if(supply[r] == 0 || demand[c] == 0){
                    System.out.printf("  -  ");
                } else{
                    System.out.printf("     ");
                }

            }

            System.out.println();
        }
        System.out.println();
    }

    public static void printSummary(){
        for (int r = 0; r < supply.length; r++) {
            for (int c = 0; c < demand.length; c++) {
                if (matrix[r][c] != null) {
                    System.out.println("Из " + (r+1) + "-го района производства направим " + matrix[r][c].quantity + "ед. груза в " + (c+1) + "-ый пункт потребления");
                }
            }
        }
    }

    public static void printSourceData(){
        System.out.println("Исходные данные:");
        System.out.print("a = [");
        for (int i = 0; i < supply.length - 1; i++) {
            System.out.print(" " + supply[i]);
        }
        System.out.println(" ]");
        System.out.print("b = [");
        for (int i : demand) {
            System.out.print(" " + i);
        }
        System.out.println(" ]");
        System.out.print("r = [");

        for (int j = 0; j < demand.length; j++) {
            System.out.print(" " + costs[supply.length - 1][j]);
        }
        System.out.println(" ]");
        System.out.println("C = ");
        for (int i = 0; i < supply.length - 1; i++){
            for (int j = 0; j < demand.length; j++){
                System.out.printf("%5s", costs[i][j]);
            }
            System.out.println();
        }
        System.out.println();
        System.out.println();

    }

    public static void main(String[] args) throws Exception {

        String filename = "input.txt";
        init(filename);
        printSourceData();
        System.out.println("Нвйдем опорный план методом северо-западного угла");
        northWestCornerRule();
        //modifiedDistribution();
        steppingStone();
        printSummary();
       // System.out.println("Оптимальный план: ");
        //printResult();

    }
}