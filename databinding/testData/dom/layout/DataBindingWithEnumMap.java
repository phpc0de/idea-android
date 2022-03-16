package p1.p2;

import android.databinding.ObservableArrayMap;

public class DataBindingWithEnumMap {
  public enum OneTwoThree {
    ONE, TWO, THREE
  }

  public ObservableArrayMap<OneTwoThree, String> map = new ObservableArrayMap<>();
}
