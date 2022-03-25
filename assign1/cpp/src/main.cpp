#include <papiStdEventDefs.h>
#include <stdio.h>
#include <iostream>
#include <fstream>
#include <iomanip>
#include <time.h>
#include <cstdlib>
#include <papi.h>
#include <string.h>
#include <queue>

void mult(int matrixSize)
{
    clock_t time1, time2;

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
    std::cout << st;

    // display 10 elements of the result matrix to verify correctness
    std::cout << "Result matrix: " << std::endl;
    for (j = 0; j < std::min(10, matrixSize); j++)
        std::cout << resultMatrix[j] << " ";
    std::cout << std::endl;

    free(leftMatrix);
    free(rightMatrix);
    free(resultMatrix);
}

void multLine(int matrixSize)
{
    clock_t time1, time2;

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
    std::cout << st;

    // display 10 elements of the result matrix to verify correctness
    std::cout << "Result matrix: " << std::endl;
    for (j = 0; j < std::min(10, matrixSize); j++)
        std::cout << resultMatrix[j] << " ";
    std::cout << std::endl;

    free(leftMatrix);
    free(rightMatrix);
    free(resultMatrix);
}

void multBlock(int matrixSize, int bkSize)
{
    clock_t time1, time2;

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
                int upperi = std::min(iBlock + bkSize, matrixSize);
                int upperj = std::min(jBlock + bkSize, matrixSize);
                int upperk = std::min(kBlock + bkSize, matrixSize);
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
    std::cout << st;

    // display 10 elements of the result matrix to verify correctness
    std::cout << "Result matrix: " << std::endl;
    for (j = 0; j < std::min(10, matrixSize); j++)
        std::cout << resultMatrix[j] << " ";
    std::cout << std::endl;

    free(leftMatrix);
    free(rightMatrix);
    free(resultMatrix);
}

int runTests(int algChoice, int eventSet)
{
    std::queue<int> matrixSizes;
    std::vector<int> blockSizes = {128, 256, 512};
    int ret;
    long long values[6] = {0, 0, 0, 0, 0, 0};
    std::string fileName;

    int matrixSize = 600;
    while (matrixSize <= 3000)
    {
        matrixSizes.push(matrixSize);
        matrixSize += 400;
    }
    switch (algChoice)
    {
    case 1:
        fileName = "lineMult.csv";
        matrixSize = 4096;
        while (matrixSize <= 10240)
        {
            matrixSizes.push(matrixSize);
            matrixSize += 2048;
        }
        break;
    case 2:
        fileName = "blockMult.csv";
        matrixSize = 4096;
        while (matrixSize <= 10240)
        {
            matrixSizes.push(matrixSize);
            matrixSize += 2048;
        }
        break;
    default:
        fileName = "mult.csv";
    }

    std::ofstream file(fileName);

    if (algChoice == 2)
    {
        file << "BLOCK SIZE,";
    }
    file << "MATRIX SIZE,L1 DCM,L2 DCM,L3 TCM,L3 TCA,L3 PCNTG,TOT INS,ELAPSED\n";

    while (!matrixSizes.empty())
    {
        clock_t time1, time2;
        double elapsed;
        matrixSize = matrixSizes.front();
        matrixSizes.pop();

        for (const auto blockSize : blockSizes)
        {
            // Start counting
            ret = PAPI_start(eventSet);
            if (ret != PAPI_OK)
                std::cout << "ERROR: Start PAPI" << std::endl;

            std::cout << std::endl;
            time1 = clock();
            switch (algChoice)
            {
            case 1:
                multLine(matrixSize);
                break;
            case 2:
                multBlock(matrixSize, blockSize);
                break;
            default:
                mult(matrixSize);
                break;
            }
            time2 = clock();
            elapsed = (double)(time2 - time1) / CLOCKS_PER_SEC;

            ret = PAPI_stop(eventSet, values);
            if (ret != PAPI_OK)
                std::cout << "ERROR: Stop PAPI" << std::endl;

            if (algChoice == 2)
            {
                file << blockSize << ",";
            }

            file << matrixSize
                 << ',' << values[0]
                 << ',' << values[1]
                 << ',' << values[2]
                 << ',' << values[3]
                 << ',' << (double)values[2] / values[3]
                 << ',' << values[4]
                 << ',' << elapsed << '\n';
            file.flush();

            if (algChoice != 2)
            {
                break; // no block sizes
            }
        }
    }

    return 0;
}

int main(int argc, char *argv[])
{
    char c = 0;
    int matrixSize = 0, blockSize;
    int op;

    int eventSet = PAPI_NULL;
    long long values[6] = {0, 0, 0, 0, 0, 0};
    int ret;

    ret = PAPI_library_init(PAPI_VER_CURRENT);
    if (ret != PAPI_VER_CURRENT)
        std::cout << "FAIL" << std::endl;

    ret = PAPI_create_eventset(&eventSet);
    if (ret != PAPI_OK)
        std::cout << "ERROR: create eventset" << PAPI_strerror(ret) << std::endl;

    ret = PAPI_add_event(eventSet, PAPI_L1_DCM);
    if (ret != PAPI_OK)
        std::cout << "ERROR: PAPI_L1_DCM" << std::endl;

    ret = PAPI_add_event(eventSet, PAPI_L2_DCM);
    if (ret != PAPI_OK)
        std::cout << "ERROR: PAPI_L2_DCM " << PAPI_strerror(ret) << std::endl;

    ret = PAPI_add_event(eventSet, PAPI_L3_TCM);
    if (ret != PAPI_OK)
        std::cout << "ERROR: PAPI_L3_TCM" << PAPI_strerror(ret) << std::endl;

    ret = PAPI_add_event(eventSet, PAPI_L3_TCA);
    if (ret != PAPI_OK)
        std::cout << "ERROR: PAPI_L3_TCA" << PAPI_strerror(ret) << std::endl;

    ret = PAPI_add_event(eventSet, PAPI_TOT_INS);
    if (ret != PAPI_OK)
        std::cout << "ERROR: PAPI_tot" << std::endl;

    op = 1;
    do
    {
        std::cout << std::endl
                  << "0. Exit" << std::endl
                  << "1. Multiplication" << std::endl;
        std::cout << "2. Line Multiplication" << std::endl;
        std::cout << "3. Block Multiplication" << std::endl;
        std::cout << "4. Run tests" << std::endl;
        std::cout << "Selection?: ";
        std::cin >> op;

        if (op == 0)
        {
            break;
        }
        else if (op == 4)
        {
            int algChoice;
            std::cout << std::endl
                      << "0. Multiplication" << std::endl;
            std::cout << "1. Line Multiplication" << std::endl;
            std::cout << "2. Block Multiplication" << std::endl
                      << "Selection? ";
            std::cin >> algChoice;
            runTests(algChoice, eventSet);
            break;
        }

        printf("Dimensions: matrixSizes=cols ? ");
        std::cin >> matrixSize;

        // Start counting
        ret = PAPI_start(eventSet);
        if (ret != PAPI_OK)
            std::cout << "ERROR: Start PAPI" << std::endl;

        switch (op)
        {
        case 1:
            mult(matrixSize);
            break;
        case 2:
            multLine(matrixSize);
            break;
        case 3:
            std::cout << "Block Size? ";
            std::cin >> blockSize;
            multBlock(matrixSize, blockSize);
            break;
        default:
            break;
        }

        ret = PAPI_stop(eventSet, values);
        if (ret != PAPI_OK)
            std::cout << "ERROR: Stop PAPI" << std::endl;
        printf("L1 DCM: %lld \n", values[0]);
        printf("L2 DCM: %lld \n", values[1]);
        printf("L3 TCM: %lld \n", values[2]);
        printf("L3 TCA: %lld \n", values[3]);
        printf("TOT INS: %lld \n", values[4]);

        ret = PAPI_reset(eventSet);
        if (ret != PAPI_OK)
            std::cout << "FAIL reset" << std::endl;

    } while (op != 0);

    ret = PAPI_remove_event(eventSet, PAPI_L1_DCM);
    if (ret != PAPI_OK)
        std::cout << "FAIL remove event" << std::endl;

    ret = PAPI_remove_event(eventSet, PAPI_L2_DCM);
    if (ret != PAPI_OK)
        std::cout << "FAIL remove event" << std::endl;

    ret = PAPI_destroy_eventset(&eventSet);
    if (ret != PAPI_OK)
        std::cout << "FAIL destroy" << std::endl;
}
