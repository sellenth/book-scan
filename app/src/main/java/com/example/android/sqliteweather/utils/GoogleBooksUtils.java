package com.example.android.sqliteweather.utils;

import android.net.Uri;
import android.util.Log;

import com.example.android.sqliteweather.data.BookEntity;
import com.example.android.sqliteweather.data.BooksResponse;
import com.google.gson.Gson;

public class GoogleBooksUtils {

    private final static String GB_BASE_URL = "https://www.googleapis.com/books/v1/volumes";
    private final static String GB_QUERY_PARAM= "q";
    private static String mISBN;


    /*
     * Set your own APPID here.
     */


    /*
     * The below several classes are used only for JSON parsing with Gson.  The main class that's
     * used to represent a single forecast item throughout the rest of the app is the ForecastItem
     * class in the data package.
     */

    public static String buildGBurl(String isbn) {
        mISBN = isbn;
        return Uri.parse(GB_BASE_URL).buildUpon()
                .appendQueryParameter(GB_QUERY_PARAM, "isbn:" + isbn)
                .build()
                .toString();
    }


    public static BookEntity parseBookJSON(String bookJSON) throws Exception {
        Gson gson = new Gson();
        BooksResponse res = gson.fromJson(bookJSON, BooksResponse.class);
        BookEntity b = new BookEntity();
        b.authors = "";
        for (int i = 0; i < res.items[0].volumeInfo.authors.length; i++){
            Log.d("authorsLen", " " + res.items[0].volumeInfo.authors[i]);
            b.authors += res.items[0].volumeInfo.authors[i];
            if (i != res.items[0].volumeInfo.authors.length-1){
                b.authors += ", ";
            }
        }
        b.isbn = mISBN;
        b.title = res.items[0].volumeInfo.title;
        b.Description = res.items[0].volumeInfo.Description;
        b.imageLinks = res.items[0].volumeInfo.imageLinks.thumbnail;
        Log.d("imageLink", b.authors);
        return b;
    }

}
