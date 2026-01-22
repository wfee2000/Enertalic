package com.wfee.enertalic.util;

public record Position(int x, int y, int z)
{
    @Override
    public boolean equals(Object other)
    {
        if (other instanceof Position(int x1, int y1, int z1))
        {
            return x1 == x &&  y1 == y && z1 == z;
        }

        return false;
    }
}
