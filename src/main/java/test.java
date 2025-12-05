
import java.util.Arrays;
import java.util.Random;

import primula.agent.AbstractAgent;

public class test extends AbstractAgent{
	int N = 10000;
	private static Random sRandom = new Random();
	public void run(){
//		String str = "";
//		String[] strs;
//		strs = new String[N];
//		byte[] asciiCodes;
//		 byte[] asciiCodes = new byte[]{65, 66, 67};
//	        String resultString;
//	        try {
//	            resultString = new String(asciiCodes, "US-ASCII");
//	        } catch (Exception e) {
//	            e.printStackTrace();
//	            return;
//	        }
//		for(int i=0;i<N;i++) {
//			str += String.valueOf(Math.floor(Math.random() * 58)+65);
//			asciiCodes = new byte[]{(byte) (Math.floor(Math.random() * 58)+65)};
//			try {
//				str += new String(asciiCodes, "US-ASCII");
//			} catch (UnsupportedEncodingException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//			strs[i] = str;
//		}
//			System.out.println("This is main computer.");
//
//			migrate();
//		System.out.println(strs);
//		System.out.println(str);
//			System.out.println("end.");
//			System.out.println("This is main computer.");

			final int n = 100000;
	        StringBuilder sb = new StringBuilder();
	        for (int i = 0; i < n; i++) {
	            sb.append(Character.forDigit(sRandom.nextInt(10), 10));
	        }
	        String s = sb.toString();
	        String[] a = new String[s.length()];
	        for (int i = 0; i < n; i++) {
	            a[i] = s.substring(i);
	        }

	        long t0 = System.nanoTime();
//	        multikeyQuicksort(a, 0, n, 0);
//	        quicksort(a, 0, n);
//	        mergesort(a, 0, n);
//	        heapsort(a, 0, n);
//	        combsort(a, 0, n);
//	        quicksort2(a, 0, n);
//	        Arrays.sort(a, new Comparator<String>() {
//	            public int compare(String o1, String o2) {
//	                return o1.compareTo(o2);
//	            }
//	        });

	        long t1 = System.nanoTime();
	        System.out.println((t1 - t0) / 1E9);

	}
	@Override
	public void requestStop() {
		// TODO 自動生成されたメソッド・スタブ
	}

	 private static <T> void swap(T[] array, int a, int b) {
	        T t = array[a];
	        array[a] = array[b];
	        array[b] = t;
	    }

	    private static int mkqCompare(String a, String b, int d) {
	        if (a.length() > d) {
	            if (b.length() > d) {
	                return a.charAt(d) - b.charAt(d);
	            } else {
	                return 1;
	            }
	        } else {
	            if (b.length() > d) {
	                return -1;
	            } else {
	                return 0;
	            }
	        }
	    }

	    private static void multikeyQuicksort(String[] array, int offset, int n, int d) {
	        if (n < 2) {
	            return;
	        }

	        String pivot = array[offset + sRandom.nextInt(n)];
	        int l = offset;
	        int h = offset + n - 1;
	        int ml = l;
	        int mh = h + 1;
	        while (true) {
	            while (l <= h) {
	                if (mkqCompare(array[l], pivot, d) > 0) {
	                    break;
	                }
	                if (mkqCompare(array[l], pivot, d) == 0) {
	                    swap(array, l, ml);
	                    ml++;
	                }
	                l++;
	            }
	            while (l <= h) {
	                if (mkqCompare(array[h], pivot, d) < 0) {
	                    break;
	                }
	                if (mkqCompare(array[h], pivot, d) == 0) {
	                    swap(array, h, mh - 1);
	                    mh--;
	                }
	                h--;
	            }
	            if (l >= h) {
	                break;
	            }
	            swap(array, l, h);
	            l++;
	            h--;
	        }

	        for (int i = 1; i <= ml - offset; i++) {
	            swap(array, ml - i, l - i);
	        }
	        for (int i = 0; i < offset + n - mh; i++) {
	            swap(array, l + i, mh + i);
	        }
	        int m = l + offset + n - mh;
	        l -= ml - offset;

	        multikeyQuicksort(array, offset, l - offset, d);
	        if (m > l && pivot.length() > d) {
	            multikeyQuicksort(array, l, m - l, d + 1);
	        }
	        multikeyQuicksort(array, m, n + offset - m, d);
	    }

	    private static <T extends Comparable<T>> void quicksort(T[] array, int offset, int n) {
	        if (n < 2) {
	            return;
	        }

	        T pivot = array[offset + sRandom.nextInt(n)];
	        int l = offset;
	        int h = offset + n - 1;
	        while (true) {
	            while (array[l].compareTo(pivot) < 0) {
	                l++;
	            }
	            while (array[h].compareTo(pivot) > 0) {
	                h--;
	            }
	            if (l >= h) {
	                break;
	            }
	            swap(array, l, h);
	            l++;
	            h--;
	        }

	        quicksort(array, offset, l - offset);
	        quicksort(array, l, offset + n - l);
	    }

	    private static <T extends Comparable<T>> void merge(T[] array, int offset, int m, int n) {
	        T[] buf = Arrays.copyOf(array, n);
	        int i = offset;
	        int j = offset + m;
	        int k = 0;
	        while (i < offset + m && j < offset + n) {
	            if (array[i].compareTo(array[j]) <= 0) {
	                buf[k++] = array[i++];
	            } else {
	                buf[k++] = array[j++];
	            }
	        }
	        while (i < offset + m) {
	            buf[k++] = array[i++];
	        }
	        while (j < offset + n) {
	            buf[k++] = array[j++];
	        }
	        System.arraycopy(buf, 0, array, offset, n);
	    }

	    private static <T extends Comparable<T>> void mergesort(T[] array, int offset, int n) {
	        if (n < 2) {
	            return;
	        }
	        int m = n / 2;
	        mergesort(array, offset, m);
	        mergesort(array, offset + m, n - m);
	        merge(array, offset, m, n);
	    }

	    private static <T extends Comparable<T>> void heapsort(T[] array, int offset, int n) {
	        for (int i = 1; i < n; i++) {
	            int j = i;
	            while (j > 0) {
	                int parent = (j - 1) / 2;
	                if (array[offset + j].compareTo(array[offset + parent]) > 0) {
	                    swap(array, offset + j, offset + parent);
	                    j = parent;
	                } else {
	                    break;
	                }
	            }
	        }
	        for (int i = n - 1; i > 0; i--) {
	            swap(array, offset, offset + i);
	            int j = 0;
	            while (true) {
	                int max = j;
	                for (int k = 1; j * 2 + k < i && k <= 2; k++) {
	                    if (array[offset + max].compareTo(array[offset + j * 2 + k]) < 0) {
	                        max = j * 2 + k;
	                    }
	                }
	                if (max == j) {
	                    break;
	                }
	                swap(array, offset + j, offset + max);
	                j = max;
	            }
	        }
	    }

	    private static <T extends Comparable<T>> void combsort(T[] array, int offset, int n) {
	        int h = n;
	        while (true) {
	            if (h == 9 || h == 10) {
	                h = 11;
	            }
	            if (h > 1) {
	                h = h * 10 / 13;
	            }
	            boolean sorted = true;
	            for (int i = offset; i + h < offset + n; i++) {
	                if (array[i].compareTo(array[i + h]) > 0) {
	                    swap(array, i, i + h);
	                    sorted = false;
	                }
	            }
	            if (h == 1 && sorted) {
	                break;
	            }
	        }
	    }

	    private static <T extends Comparable<T>> void quicksort2(T[] array, int offset, int n) {
	        if (n < 2) {
	            return;
	        }

	        swap(array, offset, offset + sRandom.nextInt(n));
	        T pivot = array[offset];
	        int l = offset + 1;
	        int h = offset + n - 1;
	        while (true) {
	            while (l <= h && array[l].compareTo(pivot) < 0) {
	                l++;
	            }
	            while (l <= h && array[h].compareTo(pivot) > 0) {
	                h--;
	            }
	            if (l >= h) {
	                break;
	            }
	            swap(array, l, h);
	            l++;
	            h--;
	        }
	        quicksort2(array, offset, l - offset);
	        quicksort2(array, l, offset + n - l);
	    }
}