objects = matrix-multiply.o

matrix-multiply : $(objects)
		   g++ -o matrix-multiply $(objects) -std=c++11 -pthread
matrix-multiply.o : matrix-multiply.cpp
		     g++ -c matrix-multiply.cpp -std=c++11 -pthread
clean :
	rm matrix-multiply $(objects)
