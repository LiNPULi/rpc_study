package com.ydlclass.impl;

public class Main {

    public static int longestMountain(int[] arr) {
        int n = arr.length;
        if (n < 3) return 0;

        int longest = 0;

        for (int i = 1; i < n - 1; ) {
            // Check if arr[i] is a peak
            if (arr[i] > arr[i - 1] && arr[i] > arr[i + 1]) {
                // Find the left boundary of the mountain
                int left = i - 1;
                while (left > 0 && arr[left] > arr[left - 1]) {
                    left--;
                }

                // Find the right boundary of the mountain
                int right = i + 1;
                while (right < n - 1 && arr[right] > arr[right + 1]) {
                    right++;
                }

                // Calculate the length of the mountain
                int length = right - left + 1;
                longest = Math.max(longest, length);

                // Move i to the right end of this mountain
                i = right + 1;
            } else {
                // Move to the next element
                i++;
            }
        }

        return longest;
    }

    public static void main(String[] args) {
        // 示例1
        int[] arr1 = {1, 1, 4, 5, 1, 4};
        System.out.println(longestMountain(arr1)); // 输出: 4

        // 示例2
        int[] arr2 = {1, 2, 2, 1};
        System.out.println(longestMountain(arr2)); // 输出: 0

        // 其他测试
        int[] arr3 = {2, 1, 4, 7, 3, 2, 5};
        System.out.println(longestMountain(arr3)); // 输出: 5

        int[] arr4 = {2, 2, 2};
        System.out.println(longestMountain(arr4)); // 输出: 0
    }
}
