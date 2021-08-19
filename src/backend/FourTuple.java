package backend;

import exception.PLDLAssemblingException;
import java.util.Arrays;
import java.util.Objects;

public class FourTuple {
    private final String tuples[];

    public String[] getTuples() {
        return tuples;
    }

    public FourTuple(String tuples) throws PLDLAssemblingException {
        String []str = tuples.split(",(?=(?:[^\"']*[\"'][^\"']*[\"'])*[^\"']*$)", -1);
        if (str.length != 4){
            throw new PLDLAssemblingException("四元式组成不为4", null);
        }
        for (int i = 0; i < 4; ++i){
            str[i] = str[i].trim();
        }
        this.tuples = str;
    }

    @Override
    public String toString() {
        return Arrays.toString(tuples);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FourTuple fourTuple = (FourTuple) o;
        return Arrays.equals(tuples, fourTuple.tuples);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(tuples);
    }
}
