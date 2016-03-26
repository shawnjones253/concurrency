// Shawn Jones
// CSCI 322, HW2
// Matrix Multiplication
// Threaded vs. non-threaded performance analysis

#include <iostream>   //cout
#include <future>     //future<>, async, launch
#include <sys/time.h> //gettimeofday, struct timeval
#include <stdlib.h>   //atoi
#include <string.h>   //strcmp

using namespace std;

double get_wallTime(){
  struct timeval tp;
  gettimeofday(&tp, NULL);
  return (double) (tp.tv_sec + tp.tv_usec/1000000.0);
}

int** createMatrix(int dim, int initVal){
  //creates a dim*dim array with all values initialized to 'initVal'
  //returns pointer to created array
  
  int** outputMatrix = new int*[dim];
  
  for (int i=0; i<dim; i++){
    outputMatrix[i] = new int[dim];
    for (int j=0; j<dim; j++){
      outputMatrix[i][j] = initVal;
    }
  }
  
  return outputMatrix;
}

int deleteMatrix(int** array, int dim){
  //deletes the dim*dim array pointed to by 'array'
  //returns 0 if successful
  
  for (int i=0; i<dim; i++){
    delete [] array[i];
  }
  delete [] array;
  
  return 0;
}

int** matrixMultiply(int** array1, int** array2, int dim) {
  //multiplies array1 * array2, each of dimension dim*dim
  //return a pointer to a new array containing result
  
  int** outputMatrix = createMatrix(dim, 0);
  
  for (int i=0; i<dim; i++){
    for (int j=0; j<dim; j++){
      int product = 0;
      for (int k=0; k<dim; k++){
        product += array1[i][k] * array2[k][j];
      }
      outputMatrix[i][j] = product;
    }
  }
  
  return outputMatrix;
}

int usage(char* argv[]) {
  cout << "Usage: " << argv[0] << " howManyTimes threading dimension [-q]" << endl;
  cout << "       howManyTimes : specifies the number of times to perform matrix multiplication" << endl;
  cout << "       threading    : can be 0 for no threading, or 1 for threading" << endl;
  cout << "       dimension    : specifies the dimension of matrices to be multiplied" << endl;
  cout << "       -q           : optional, when used output will consist only of matrixMultiply time" << endl << endl;
}

int main(int argc, char* argv[]){
  
  if ( !( (argc == 4) || ((argc == 5) && (strcmp(argv[4], "-q") == 0)) ) ){
    usage(argv);
    return 1;
  }
  
  //initialize variables
  double t1 = 0, t2 = 0, t3 = 0, t4 = 0;
  int numTimes = atoi(argv[1]);
  int useThreads = atoi(argv[2]);
  int dim = atoi(argv[3]);
  int** myResults[numTimes];
  int q;
  ((argc == 5) && (strcmp(argv[4], "-q") == 0)) ? q=1 : q=0;
  
  //output parameters for user
  if (!q){
    cout << endl << "Running matrixMultiply " << numTimes << " times" << endl;
    cout << " threading ";
    if (useThreads){
      cout << "enabled" << endl;
    }
    else{
      cout << "disabled" << endl;
    }
    cout << " " << dim << "*" << dim << " matrices" << endl << endl;
  }
  
  //start time for array init
  t1 = get_wallTime();

  int** array1 = createMatrix(dim, 1);
  int** array2 = createMatrix(dim, 2);

  //end time for array init
  t2 = get_wallTime();

  if (!q){
    cout << "Init time : " << t2-t1 << endl;
  }

  
  //start time for multiply
  t3 = get_wallTime();
  
  if (useThreads) {
    //threaded
    future<int**> myThreads[numTimes];
    
    //launch threads
    for (int i=0; i<numTimes; i++){
      myThreads[i] = async(launch::async, matrixMultiply, array1, array2, dim);
    }
    //collect results
    for (int i=0; i<numTimes; i++){
      myResults[i] = myThreads[i].get();
    }
  }
  else {
    //non-threaded
    for (int i=0; i<numTimes; i++){
      myResults[i] = matrixMultiply(array1, array2, dim);
    }
  }
  
  //end time for multiply
  t4 = get_wallTime();
  
  if (!q){
    cout << "matrixMultiply time : " << t4-t3 << endl << endl;
  }
  else{
    cout << t4-t3;
  }
  
  
  //cleanup
  deleteMatrix(array1, dim);
  deleteMatrix(array2, dim);
  for (int** array : myResults){
    deleteMatrix(array, dim);
  }
  
  return 0;
}
