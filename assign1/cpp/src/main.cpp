#include <papiStdEventDefs.h>
#include <stdio.h>
#include <iostream>
#include <fstream>
#include <iomanip>
#include <time.h>
#include <cstdlib>
#include <papi.h>
#include <string.h>

using namespace std;

#define SYSTEMTIME clock_t

void mult(int matrixSize)
{
    SYSTEMTIME time1, time2;

    char st[100];
    double temp;
    int i, j, k;

    double *leftMatrix, *rightMatrix, *resultMatrix;

    leftMatrix = (double *)malloc((matrixSize * matrixSize) * sizeof(double));
    rightMatrix = (double *)malloc((matrixSize * matrixSize) * sizeof(double));
    resultMatrix = (double *)malloc((matrixSize * matrixSize) * sizeof(double));

    for (i = 0; i < matrixSize; i++)
        for (j = 0; j < matrixSize; j++)
            leftMatrix[i * matrixSize + j] = (double)1.0;

    for (i = 0; i < matrixSize; i++)
        for (j = 0; j < matrixSize; j++)
            rightMatrix[i * matrixSize + j] = (double)(i + 1);

    time1 = clock();

    for (i = 0; i < matrixSize; i++)
    {
        for (j = 0; j < matrixSize; j++)
        {
            temp = 0;
            for (k = 0; k < matrixSize; k++)
            {
                temp += leftMatrix[i * matrixSize + k] * rightMatrix[k * matrixSize + j];
            }
            resultMatrix[i * matrixSize + j] = temp;
        }
    }

    time2 = clock();
    sprintf(st, "Time: %3.3f seconds\n", (double)(time2 - time1) / CLOCKS_PER_SEC);
    cout << st;

    // display 10 elements of the result matrix tto verify correctness
    cout << "Result matrix: " << endl;
    for (i = 0; i < 1; i++)
    {
        for (j = 0; j < min(10, matrixSize); j++)
            cout << resultMatrix[j] << " ";
    }
    cout << endl;

    free(leftMatrix);
    free(rightMatrix);
    free(resultMatrix);
}

void multLine(int matrixSize)
{
    SYSTEMTIME time1, time2;

    char st[100];
    double temp;
    int i, j, k;

    double *leftMatrix, *rightMatrix, *resultMatrix;

    leftMatrix = (double *)malloc((matrixSize * matrixSize) * sizeof(double));
    rightMatrix = (double *)malloc((matrixSize * matrixSize) * sizeof(double));
    resultMatrix = (double *)malloc((matrixSize * matrixSize) * sizeof(double));
    memset(resultMatrix, 0, (matrixSize * matrixSize) * sizeof(double));

    for (i = 0; i < matrixSize; i++)
        for (j = 0; j < matrixSize; j++)
            leftMatrix[i * matrixSize + j] = (double)1.0;

    for (i = 0; i < matrixSize; i++)
        for (j = 0; j < matrixSize; j++)
            rightMatrix[i * matrixSize + j] = (double)(i + 1);

    time1 = clock();

    for (i = 0; i < matrixSize; i++)
    {
        for (k = 0; k < matrixSize; k++)
        {
            for (j = 0; j < matrixSize; j++)
            {
                resultMatrix[i * matrixSize + j] += leftMatrix[i * matrixSize + k] * rightMatrix[k * matrixSize + j];
            }
        }
    }

    time2 = clock();
    sprintf(st, "Time: %3.3f seconds\n", (double)(time2 - time1) / CLOCKS_PER_SEC);
    cout << st;

    // display 10 elements of the result matrix tto verify correctness
    cout << "Result matrix: " << endl;
    for (i = 0; i < 10; i++)
    {
        for (j = 0; j < min(10, matrixSize); j++)
            cout << resultMatrix[j] << " ";
    }
    cout << endl;

    free(leftMatrix);
    free(rightMatrix);
    free(resultMatrix);
}

void multBlock(int matrixSize, int bkSize)
{
    SYSTEMTIME time1, time2;

    char st[100];
    double temp;
    int i, j, k;

    double *leftMatrix, *rightMatrix, *resultMatrix;

    leftMatrix = (double *)malloc((matrixSize * matrixSize) * sizeof(double));
    rightMatrix = (double *)malloc((matrixSize * matrixSize) * sizeof(double));
    resultMatrix = (double *)malloc((matrixSize * matrixSize) * sizeof(double));
    memset(resultMatrix, 0, (matrixSize * matrixSize) * sizeof(double));

    for (i = 0; i < matrixSize; i++)
        for (j = 0; j < matrixSize; j++)
            leftMatrix[i * matrixSize + j] = (double)1.0;

    for (i = 0; i < matrixSize; i++)
        for (j = 0; j < matrixSize; j++)
            rightMatrix[i * matrixSize + j] = (double)(i + 1);

    time1 = clock();
    int sum, iBlock, jBlock, kBlock;
    for (iBlock = 0; iBlock < matrixSize; iBlock += bkSize)
    {
        for (jBlock = 0; jBlock < matrixSize; jBlock += bkSize)
        {
            for (kBlock = 0; kBlock < matrixSize; kBlock += bkSize)
            {
                int upperi = min(iBlock + bkSize, matrixSize);
                int upperj = min(jBlock + bkSize, matrixSize);
                int upperk = min(kBlock + bkSize, matrixSize);
                for (i = iBlock; i < upperi; i++)
                {
                    for (k = kBlock; k < upperk; k++)
                    {
                        for (j = jBlock; j < upperj; j++)
                        {
                            resultMatrix[i * matrixSize + j] +=
                                leftMatrix[i * matrixSize + k] * rightMatrix[k * matrixSize + j];
                        }
                    }
                }
            }
        }
    }

    time2 = clock();
    sprintf(st, "Time: %3.3f seconds\n", (double)(time2 - time1) / CLOCKS_PER_SEC);
    cout << st;

    // display 10 elements of the result matrix tto verify correctness
    cout << "Result matrix: " << endl;
    for (i = 0; i < 10; i++)
    {
        for (j = 0; j < min(10, matrixSize); j++)
            cout << resultMatrix[j] << " ";
    }
    cout << endl;

    free(leftMatrix);
    free(rightMatrix);
    free(resultMatrix);
}

void handle_error(int retval)
{
    printf("PAPI error %d: %s\n", retval, PAPI_strerror(retval));
    exit(1);
}

void init_papi()
{
    int retval = PAPI_library_init(PAPI_VER_CURRENT);
    if (retval != PAPI_VER_CURRENT && retval < 0)
    {
        printf("PAPI library version mismatch!\n");
        exit(1);
    }
    if (retval < 0)
        handle_error(retval);

    std::cout << "PAPI Version Number: MAJOR: " << PAPI_VERSION_MAJOR(retval)
              << " MINOR: " << PAPI_VERSION_MINOR(retval)
              << " REVISION: " << PAPI_VERSION_REVISION(retval) << "\n";
}

int run_tests(int start_size, int max_size, int start_block_size, int max_block, int EventSet) {
    int matrix_size = start_size;
    int block_size = start_block_size;
    int ret;
    long long values[6] = {0,0,0,0,0,0};
    ofstream multFile("multFile.csv");
    ofstream lineMultFile("lineMultFile.csv");
    // ofstream blockMultFile("multFile.csv");

    multFile << "SIZE,L1 DCM,L2 DCM,L3 TCM,L3 TCA,L3 PCNTG,TOT INS\n";
    lineMultFile << "SIZE,L1 DCM,L2 DCM,L3 TCM,L3 TCA,L3 PCNTG,TOT INS\n";

    while(matrix_size <= max_size) {
        // Start counting
        ret = PAPI_start(EventSet);
        if (ret != PAPI_OK)
            cout << "ERROR: Start PAPI" << endl;

        mult(matrix_size);

        ret = PAPI_stop(EventSet, values);
        if (ret != PAPI_OK)
            cout << "ERROR: Stop PAPI" << endl;

        multFile << matrix_size 
        << ',' << values[0] 
        << ',' << values[1] 
        << ',' << values[2] 
        << ',' << values[3] 
        << ',' << (double)values[2] / values [3]
        << ',' << values[4] << '\n';

        ret = PAPI_reset(EventSet);
        if (ret != PAPI_OK)
            std::cout << "FAIL reset" << endl;

        // Start counting
        ret = PAPI_start(EventSet);
        if (ret != PAPI_OK)
            cout << "ERROR: Start PAPI" << endl;

        multLine(matrix_size);

        ret = PAPI_stop(EventSet, values);
        if (ret != PAPI_OK)
            cout << "ERROR: Stop PAPI" << endl;
        lineMultFile << matrix_size 
        << ',' << values[0] 
        << ',' << values[1] 
        << ',' << values[2] 
        << ',' << values[3] 
        << ',' << (double)values[2] / values [3]
        << ',' << values[4] << '\n';

        ret = PAPI_reset(EventSet);
        if (ret != PAPI_OK)
            std::cout << "FAIL reset" << endl;
        matrix_size *= 2;
    }

    multFile.close();
    lineMultFile.close();
    return 0;
}

int main(int argc, char *argv[])
{

    char c = 0;
    int matrixSize = 0, blockSize;
    int op;

    int EventSet = PAPI_NULL;
    long long values[6] = {0,0,0,0,0,0};
    int ret;

    ret = PAPI_library_init(PAPI_VER_CURRENT);
    if (ret != PAPI_VER_CURRENT)
        std::cout << "FAIL" << endl;

    ret = PAPI_create_eventset(&EventSet);
    if (ret != PAPI_OK)
        cout << "ERROR: create eventset" << PAPI_strerror(ret)  << endl;

    ret = PAPI_add_event(EventSet, PAPI_L1_DCM);
    if (ret != PAPI_OK)
        cout << "ERROR: PAPI_L1_DCM" << endl;

    ret = PAPI_add_event(EventSet, PAPI_L2_DCM);
    if (ret != PAPI_OK)
        cout << "ERROR: PAPI_L2_DCM " << PAPI_strerror(ret)  << endl;

    ret = PAPI_add_event(EventSet, PAPI_L3_TCM);
    if (ret != PAPI_OK)
        cout << "ERROR: PAPI_L3_TCM"  << PAPI_strerror(ret)  << endl;

    ret = PAPI_add_event(EventSet, PAPI_L3_TCA);
    if (ret != PAPI_OK)
        cout << "ERROR: PAPI_L3_TCA" << PAPI_strerror(ret)  << endl;

    ret = PAPI_add_event(EventSet, PAPI_TOT_INS);
    if (ret != PAPI_OK)
        cout << "ERROR: PAPI_tot" << endl;

    op = 1;
    do
    {
        cout << endl
             << "0. Exit" << endl
             << "1. Multiplication" << endl;
        cout << "2. Line Multiplication" << endl;
        cout << "3. Block Multiplication" << endl;
        cout << "4. Store Values" << endl;
        cout << "Selection?: ";
        cin >> op;
        if (op == 0)
            break;

        if(op == 4) {
            run_tests(512, 1024, 64, 512, EventSet);
            break;
        }
        printf("Dimensions: matrixSizes=cols ? ");
        cin >> matrixSize;

        // Start counting
        ret = PAPI_start(EventSet);
        if (ret != PAPI_OK)
            cout << "ERROR: Start PAPI" << endl;

        switch (op)
        {
        case 1:
            mult(matrixSize);
            break;
        case 2:
            multLine(matrixSize);
            break;
        case 3:
            cout << "Block Size? ";
            cin >> blockSize;
            multBlock(matrixSize, blockSize);
            break;
        }

        ret = PAPI_stop(EventSet, values);
        if (ret != PAPI_OK)
            cout << "ERROR: Stop PAPI" << endl;
        printf("L1 DCM: %lld \n", values[0]);
        printf("L2 DCM: %lld \n", values[1]);
        printf("L3 TCM: %lld \n", values[2]);
        printf("L3 TCA: %lld \n", values[3]);
        printf("TOT INS: %lld \n", values[4]);

        ret = PAPI_reset(EventSet);
        if (ret != PAPI_OK)
            std::cout << "FAIL reset" << endl;

    } while (op != 0);

    ret = PAPI_remove_event(EventSet, PAPI_L1_DCM);
    if (ret != PAPI_OK)
        std::cout << "FAIL remove event" << endl;

    ret = PAPI_remove_event(EventSet, PAPI_L2_DCM);
    if (ret != PAPI_OK)
        std::cout << "FAIL remove event" << endl;

    ret = PAPI_destroy_eventset(&EventSet);
    if (ret != PAPI_OK)
        std::cout << "FAIL destroy" << endl;
}

/**

2 --- 4096

Time: 76.840 seconds
Result matrix:
8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06
L1 DCM: 17725381879
L2 DCM: 18130499064
TOT INS: 550024917538

3 --- 4096 x 128

Time: 66.931 seconds
Result matrix:
8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06 8.39066e+06
L1 DCM: 9760764472
L2 DCM: 30154685498
TOT INS: 487136182659







2 --- 2048 --

Time: 11.014 seconds
L1 DCM: 1141916442
L2 DCM: 2229068626
L3 TCM: 1456183770
L3 TCA: 2229148269
L3 DCA: 2229068626

3 --- 2048 -- 128

Time: 7.815 seconds
L1 DCM: 1220012714
L2 DCM: 3561164020
L3 TCM: 162716262
L3 TCA: 3561186799
L3 DCA: 3561164020

3 ---- 2048 -- 256

Time: 6.865 seconds
L1 DCM: 1138199254
L2 DCM: 2786496234
L3 TCM: 44412140
L3 TCA: 2786519784
L3 DCA: 2786496234


*/