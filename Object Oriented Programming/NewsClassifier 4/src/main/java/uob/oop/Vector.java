package uob.oop;

public class Vector {
    private double[] doubElements;

    public Vector(double[] _elements) {
        //TODO Task 3.1 - 0.5 marks
        doubElements = _elements;
    }

    public double getElementatIndex(int _index) {
        //TODO Task 3.2 - 2 marks
        if (_index >= 0 && _index < doubElements.length){
            return doubElements[_index];
        }
        else {
            return -1.0; //you need to modify the return value
        }
    }

    public void setElementatIndex(double _value, int _index) {
        //TODO Task 3.3 - 2 marks
        if (_index >= 0 && _index < doubElements.length){
            doubElements[_index] = _value;
        }
        else {
            if (doubElements.length > 0){
                doubElements[doubElements.length - 1] = _value;
            }
            else {
                throw new IndexOutOfBoundsException("Array is empty. Cannot set an element.");
            }
        }
    }

    public double[] getAllElements() {
        //TODO Task 3.4 - 0.5 marks
        return doubElements; //you need to modify the return value
    }

    public int getVectorSize() {
        //TODO Task 3.5 - 0.5 marks
        return doubElements.length; //you need to modify the return value
    }

    public Vector reSize(int _size) {
        //TODO Task 3.6 - 6 marks
        if (_size == doubElements.length || _size <= 0){
            return this;
        }
        else {

            double[] resizedDoubElements = new double[_size];
            if (_size < doubElements.length){
                System.arraycopy(doubElements, 0, resizedDoubElements, 0, _size);
            }
            else {
                System.arraycopy(doubElements, 0, resizedDoubElements, 0, doubElements.length);

                for (int index = doubElements.length; index < _size; index++) {
                    resizedDoubElements[index] = -1.0;
                }
            }
            return new Vector(resizedDoubElements);
        }
    }

    public Vector add(Vector _v) {
        //TODO Task 3.7 - 2 marks
        if (_v.getVectorSize() > this.getVectorSize()){
            this.reSize(_v.getVectorSize());
            double[] additionVector = new double[_v.getVectorSize()];
            for (int i = 0; i < _v.getVectorSize(); i++){
                additionVector[i] =  this.getElementatIndex(i) +_v.getElementatIndex(i);
            }
            return new Vector(additionVector);
        }
        else {
            _v.reSize(this.getVectorSize());
            double[] additionVector = new double[this.getVectorSize()];
            for (int i = 0; i < this.getVectorSize(); i++){
                additionVector[i] = this.getElementatIndex(i) + _v.getElementatIndex(i);
            }
            return new Vector(additionVector);
        }
    }

    public Vector subtraction(Vector _v) {
        //TODO Task 3.8 - 2 marks
        if (_v.getVectorSize() > this.getVectorSize()){
            this.reSize(_v.getVectorSize());
            double[] subtractionVector = new double[_v.getVectorSize()];
            for (int i = 0; i < _v.getVectorSize(); i++){
                subtractionVector[i] = this.getElementatIndex(i) - _v.getElementatIndex(i);
            }
            return new Vector(subtractionVector);
        }
        else {
            _v.reSize(this.getVectorSize());
            double[] subtractionVector = new double[this.getVectorSize()];
            for (int i = 0; i < this.getVectorSize(); i++){
                subtractionVector[i] = this.getElementatIndex(i) - _v.getElementatIndex(i);
            }
            return new Vector(subtractionVector);
        }
    }

    public double dotProduct(Vector _v) {
        //TODO Task 3.9 - 2 marks
        if (_v.getVectorSize() > this.getVectorSize()){
            this.reSize(_v.getVectorSize());
            double[] dotProductArray = new double[_v.getVectorSize()];
            for (int i = 0; i < _v.getVectorSize(); i++){
                dotProductArray[i] = this.getElementatIndex(i) * _v.getElementatIndex(i);
            }
            double dotProductVector = 0.0;
            for (double element: dotProductArray){
                dotProductVector += element;
            }
            return dotProductVector;
        }
        else {
            _v.reSize(this.getVectorSize());
            double[] dotProductArray = new double[this.getVectorSize()];
            for (int i = 0; i < this.getVectorSize(); i++){
                dotProductArray[i] = this.getElementatIndex(i) * _v.getElementatIndex(i);
            }
            double dotProductVector = 0.0;
            for (double element: dotProductArray){
                dotProductVector += element;
            }
            return dotProductVector;
        }
    }

    public double cosineSimilarity(Vector _v) {
        //TODO Task 3.10 - 6.5 marks
        if (_v.getVectorSize() > this.getVectorSize()) {
            this.reSize(_v.getVectorSize());

            double[] squareThis = new double[_v.getVectorSize()];
            for (int thisElement = 0; thisElement < _v.getVectorSize(); thisElement++) {
                squareThis[thisElement] = Math.pow(this.getElementatIndex(thisElement), 2);
            }

            double sumOfThis = 0.0;
            for (double squareThisElement : squareThis) {
                sumOfThis += squareThisElement;
            }

            double[] squareV = new double[_v.getVectorSize()];
            for (int vElement = 0; vElement < _v.getVectorSize(); vElement++) {
                squareV[vElement] = Math.pow(_v.getElementatIndex(vElement), 2);
            }

            double sumOfV = 0.0;
            for (double squareVElement : squareV) {
                sumOfV += squareVElement;
            }
            return dotProduct(_v) / (Math.sqrt(sumOfThis) * Math.sqrt(sumOfV));
        }
        else {
            _v.reSize(this.getVectorSize());

            double[] squareThis = new double[this.getVectorSize()];
            for (int thisElement = 0; thisElement < this.getVectorSize(); thisElement++) {
                squareThis[thisElement] = Math.pow(this.getElementatIndex(thisElement), 2);
            }

            double sumOfThis = 0.0;
            for (double squareThisElement : squareThis) {
                sumOfThis += squareThisElement;
            }

            double[] squareV = new double[this.getVectorSize()];
            for (int vElement = 0; vElement < this.getVectorSize(); vElement++) {
                squareV[vElement] = Math.pow(_v.getElementatIndex(vElement), 2);
            }

            double sumOfV = 0.0;
            for (double squareVElement : squareV) {
                sumOfV += squareVElement;
            }
            return dotProduct(_v) / (Math.sqrt(sumOfThis) * Math.sqrt(sumOfV));
        }
    }

    @Override
    public boolean equals(Object _obj) {
        Vector v = (Vector) _obj;
        boolean boolEquals = true;

        if (this.getVectorSize() != v.getVectorSize())
            return false;

        for (int i = 0; i < this.getVectorSize(); i++) {
            if (this.getElementatIndex(i) != v.getElementatIndex(i)) {
                boolEquals = false;
                break;
            }
        }
        return boolEquals;
    }

    @Override
    public String toString() {
        StringBuilder mySB = new StringBuilder();
        for (int i = 0; i < this.getVectorSize(); i++) {
            mySB.append(String.format("%.5f", doubElements[i])).append(",");
        }
        mySB.delete(mySB.length() - 1, mySB.length());
        return mySB.toString();
    }
}
