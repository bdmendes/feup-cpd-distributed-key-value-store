#include <stdio.h>
#include <iostream>
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

// add code here for matrixSizee x matrixSizee matriz multiplication
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
                resultMatrix[i * matrixSize + j] += leftMatrix[i * matrixSize + k] * rightMatrix[k * matrixSize + j]; // leftMatrix[i][k] * rightMatrix[k][j]
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

// add code here for block x block matriz multiplication
void multBlock(int matrixSize, int bkSize) {}

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

int main(int argc, char *argv[])
{

    char c;
    int matrixSize, blockSize;
    int op;

    int EventSet = PAPI_NULL;
    long long values[2];
    int ret;

    ret = PAPI_library_init(PAPI_VER_CURRENT);
    if (ret != PAPI_VER_CURRENT)
        std::cout << "FAIL" << endl;

    ret = PAPI_create_eventset(&EventSet);
    if (ret != PAPI_OK)
        cout << "ERROR: create eventset" << endl;

    ret = PAPI_add_event(EventSet, PAPI_L1_DCM);
    if (ret != PAPI_OK)
        cout << "ERROR: PAPI_L1_DCM" << endl;

    ret = PAPI_add_event(EventSet, PAPI_L2_DCM);
    if (ret != PAPI_OK)
        cout << "ERROR: PAPI_L2_DCM" << endl;

    op = 1;
    do
    {
        cout << endl
             << "0. Exit" << endl
             << "1. Multiplication" << endl;
        cout << "2. Line Multiplication" << endl;
        cout << "3. Block Multiplication" << endl;
        cout << "Selection?: ";
        cin >> op;
        if (op == 0)
            break;
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
