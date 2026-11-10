import java.util.Locale;
import java.util.Scanner;

public class App {
    // Margen de error permitido para comparar decimales en punto flotante
    private static final double TOLERANCIA = 1e-6;
    // Límite de pasos para determinar si una matriz oscila y no converge
    private static final int MAX_ITERACIONES = 10000;

    public static void main(String[] args) throws Exception {
        Scanner scanner = new Scanner(System.in).useLocale(Locale.US);

        System.out.println("=== SIMULADOR DE CADENAS DE MARKOV ===");

        // =========================================================================
        // PUNTO 1: Solicitarle al usuario la matriz estocástica que desea evaluar
        // =========================================================================
        double[][] matrizP = solicitarMatrizEstocastica(scanner);

        System.out.println("\nMatriz de transición P ingresada:");
        imprimirMatriz(matrizP);

        
        // PUNTO 2: Determinar si es posible alcanzar una distribución de estado estable
        double[][] matrizLimite = calcularConvergencia(matrizP);

        if (matrizLimite != null) {
            System.out.println("\n[RESULTADO PUNTO 2]: SÍ es posible alcanzar una distribución de estado estable.");
            System.out.println("La matriz convergió a una única distribución límite (cadena ergódica).");

            
            // PUNTO 3: Si el ítem 2 es positivo, calcular el estado estable y mostrarlo
            // Se extrae directamente de la matriz límite hallada en el Punto 2
            double[] estadoEstable = obtenerEstadoEstableDeMatrizLimite(matrizLimite);

            System.out.println("\nMatriz Límite (P^t cuando t tiende a infinito):");
            imprimirMatriz(matrizLimite);

            System.out.println("\n[RESULTADO PUNTO 3]: Distribución de Estado Estable (Vector Pi):");
            imprimirVectorPi(estadoEstable);

            // =========================================================================
            // PUNTO 4: Calcular y mostrar el Mixing time
            // =========================================================================
            // Tolerancia de 0.25 definida en el documento de especificaciones
            calcularMixingTime(matrizP, estadoEstable, 0.25);

        } else {
            System.out.println("\n[RESULTADO PUNTO 2]: NO es posible alcanzar una única distribución de estado estable.");
            System.out.println("Motivo: La cadena es periódica (no se estabiliza) o es reducible con múltiples estados absorbentes.");
        }

        // =========================================================================
        // PUNTO 5: Incluir una funcionalidad particular (Clasificación de Estados)
        // =========================================================================
        clasificarEstados(matrizP);

        scanner.close();
    }
    
   /**
     * PUNTO 1: Solicita por consola el tamaño y los valores de la matriz,
     * validando que cumpla las reglas de una matriz estocástica.
     */
    public static double[][] solicitarMatrizEstocastica(Scanner scanner) {
        int n = 0;
        while (n < 2) {
            System.out.print("Ingrese el número de estados de la cadena (n >= 2): ");
            if (scanner.hasNextInt()) {
                n = scanner.nextInt();
                if (n < 2) {
                    System.out.println("Error: La cadena debe tener al menos 2 estados.");
                }
            } else {
                System.out.println("Error: Debe ingresar un número entero.");
                scanner.next();
            }
        }

        double[][] matriz = new double[n][n];
        boolean matrizValida = false;

        // El ciclo se repite hasta que toda la matriz cumpla las reglas estocásticas
        while (!matrizValida) {
            System.out.println("\nIngrese las probabilidades fila por fila (use punto decimal, ej. 0.25):");
            matrizValida = true;

            for (int i = 0; i < n; i++) {
                double sumaFila = 0.0;
                System.out.println("--- Fila del Estado " + i + " ---");

                for (int j = 0; j < n; j++) {
                    System.out.print("P[" + i + "][" + j + "] (Transición de " + i + " a " + j + "): ");
                    double valor = scanner.nextDouble();

                    // Validación 1: Probabilidad entre 0 y 1
                    if (valor < 0.0 || valor > 1.0) {
                        System.out.println("Error: Las probabilidades deben estar entre 0.0 y 1.0.");
                        matrizValida = false;
                        break;
                    }

                    matriz[i][j] = valor;
                    sumaFila += valor;
                }

                if (!matrizValida) {
                    break; // Si falló un valor, reinicia la lectura de la matriz
                }

                // Validación 2: La suma de las probabilidades de cada fila debe ser 1.0
                if (Math.abs(sumaFila - 1.0) > TOLERANCIA) {
                    System.out.printf("Error: La fila %d suma %.4f. Debe sumar exactamente 1.0.\n", i, sumaFila);
                    matrizValida = false;
                    break;
                }
            }
        }
        return matriz;
    }

    /**
     * PUNTO 2: Evalúa si al multiplicar la matriz P sucesivamente por sí misma (P^t)
     * se estabiliza en el tiempo y todas sus filas se vuelven iguales.
     */
    public static double[][] calcularConvergencia(double[][] P) {
        double[][] actual = copiarMatriz(P);

        for (int paso = 1; paso <= MAX_ITERACIONES; paso++) {
            // Calcular el paso siguiente: P^(t+1) = P^t * P
            double[][] siguiente = multiplicarMatrices(actual, P);

            // Condición A: ¿Dejó de cambiar la matriz entre el paso t y el paso t+1?
            if (matricesIguales(actual, siguiente, TOLERANCIA)) {
                // Condición B: ¿Todas las filas convergen a los mismos valores?
                if (filasIguales(siguiente, TOLERANCIA)) {
                    return siguiente; // Converge: retorna la matriz límite
                } else {
                    return null; // No hay estado estable único (depende de dónde inicie)
                }
            }
            actual = siguiente;
        }

        // Si llegó a MAX_ITERACIONES sin estabilizarse, es una cadena periódica
        return null;
    }
    
    /**
     * Obtiene el vector de estado estable extrayendo la primera
     * fila de la matriz límite calculada en el Punto 2.
     */
    public static double[] obtenerEstadoEstableDeMatrizLimite(double[][] matrizLimite) {
        int n = matrizLimite.length;
        double[] pi = new double[n];

        // Como todas las filas de matrizLimite ya son iguales, copiamos la fila 0
        for (int j = 0; j < n; j++) {
            pi[j] = matrizLimite[0][j];
        }

        return pi;
    }


    // =========================================================================
    // MÉTODOS DEL PUNTO 4 Y 5 (Desarrollados por ti)
    // =========================================================================

    /**
     * PUNTO 4: Calcula y muestra el Mixing Time generando una tabla de convergencia.
     */
    public static void calcularMixingTime(double[][] P, double[] pi, double tolerancia) {
        double[][] Pt = copiarMatriz(P);
        int iteracion = 1;
        int limite = 100000; // Límite de seguridad
        double distancia = 1.0;

        System.out.println("\n[RESULTADO PUNTO 4]: Cálculo del Mixing Time");
        System.out.println("Tolerancia definida: " + tolerancia);
        System.out.println("Iteración | Distancia de Variación Total (TVD)");
        System.out.println("----------------------------------------------");

        while (iteracion <= limite) {
            distancia = calcularTVD(Pt, pi);
            
            System.out.printf("%9d | %.6f\n", iteracion, distancia);

            if (distancia < tolerancia) {
                System.out.println("\n=> Mixing Time alcanzado en la iteración: " + iteracion);
                break;
            }

            Pt = multiplicarMatrices(Pt, P);
            iteracion++;
        }

        if (iteracion > limite) {
            System.out.println("\nSe alcanzó el límite de seguridad de " + limite + " iteraciones sin converger.");
        }
    }

    /**
     * Método auxiliar para calcular la Distancia de Variación Total (TVD)
     */
    private static double calcularTVD(double[][] Pt, double[] pi) {
        int n = Pt.length;
        double maxTVD = 0.0;
        
        for (int i = 0; i < n; i++) {
            double suma = 0.0;
            for (int j = 0; j < n; j++) {
                suma += Math.abs(Pt[i][j] - pi[j]);
            }
            double tvdFila = suma / 2.0;
            if (tvdFila > maxTVD) {
                maxTVD = tvdFila;
            }
        }
        return maxTVD;
    }

    /**
     * PUNTO 5: Identifica y etiqueta cada estado.
     */
    public static void clasificarEstados(double[][] P) {
        System.out.println("\n[RESULTADO PUNTO 5]: Clasificación Completa de los Estados");
        int n = P.length;
        boolean[][] alcanzabilidad = calcularMatrizAlcanzabilidad(P);

        for (int i = 0; i < n; i++) {
            boolean esAbsorbente = (Math.abs(P[i][i] - 1.0) < TOLERANCIA);
            boolean esRecurrente = true;

            if (!esAbsorbente) {
                for (int j = 0; j < n; j++) {
                    if (alcanzabilidad[i][j] && !alcanzabilidad[j][i]) {
                        esRecurrente = false;
                        break;
                    }
                }
            }

            System.out.print("Estado " + i + ": ");
            if (esAbsorbente) {
                System.out.println("Absorbente (Periodo: 1)");
            } else if (esRecurrente) {
                System.out.println("Recurrente");
            } else {
                System.out.println("Transitorio");
            }
        }
    }

    /**
     * Método auxiliar para generar la matriz de alcanzabilidad utilizando Warshall.
     */
    private static boolean[][] calcularMatrizAlcanzabilidad(double[][] P) {
        int n = P.length;
        boolean[][] R = new boolean[n][n];

        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (P[i][j] > 0) {
                    R[i][j] = true;
                }
            }
            R[i][i] = true; 
        }

        for (int k = 0; k < n; k++) {
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    R[i][j] = R[i][j] || (R[i][k] && R[k][j]);
                }
            }
        }
        return R;
    }


    // =========================================================================
    // MÉTODOS AUXILIARES DE OPERACIONES MATRICIALES Y SALIDA POR CONSOLA
    // =========================================================================

    public static double[][] multiplicarMatrices(double[][] A, double[][] B) {
        int n = A.length;
        double[][] C = new double[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                for (int k = 0; k < n; k++) {
                    C[i][j] += A[i][k] * B[k][j];
                }
            }
        }
        return C;
    }

    public static boolean matricesIguales(double[][] A, double[][] B, double tol) {
        int n = A.length;
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (Math.abs(A[i][j] - B[i][j]) > tol) {
                    return false;
                }
            }
        }
        return true;
    }

    public static boolean filasIguales(double[][] M, double tol) {
        int n = M.length;
        for (int i = 1; i < n; i++) {
            for (int j = 0; j < n; j++) {
                // Compara la fila i contra la fila 0
                if (Math.abs(M[i][j] - M[0][j]) > tol) {
                    return false;
                }
            }
        }
        return true;
    }

    public static double[][] copiarMatriz(double[][] M) {
        int n = M.length;
        double[][] copia = new double[n][n];
        for (int i = 0; i < n; i++) {
            System.arraycopy(M[i], 0, copia[i], 0, n);
        }
        return copia;
    }

    public static void imprimirMatriz(double[][] M) {
        for (double[] fila : M) {
            System.out.print("[ ");
            for (double valor : fila) {
                System.out.printf("%.4f ", valor);
            }
            System.out.println("]");
        }
    }

    public static void imprimirVectorPi(double[] pi) {
        System.out.print("Pi = [ ");
        for (int i = 0; i < pi.length; i++) {
            System.out.printf("Estado %d: %.4f (%.2f%%)%s",
                    i, pi[i], pi[i] * 100, (i < pi.length - 1 ? " | " : " "));
        }
        System.out.println("]");
    }
}