package com.rajiv.paisatrack.data

import android.content.Context
import org.json.JSONArray
import java.io.File

/** Dead-simple persistence: a JSON file in the app's private storage. */
object Store {
    private const val FILE = "txns.json"

    @Synchronized
    fun load(ctx: Context): MutableList<Txn> {
        val f = File(ctx.filesDir, FILE)
        if (!f.exists()) return mutableListOf()
        return try {
            val arr = JSONArray(f.readText())
            MutableList(arr.length()) { Txn.fromJson(arr.getJSONObject(it)) }
        } catch (e: Exception) {
            mutableListOf()
        }
    }

    @Synchronized
    fun save(ctx: Context, list: List<Txn>) {
        val arr = JSONArray()
        list.forEach { arr.put(it.toJson()) }
        File(ctx.filesDir, FILE).writeText(arr.toString())
    }

    /** Adds new txns, skipping any whose id already exists. Returns how many were added. */
    @Synchronized
    fun addUnique(ctx: Context, incoming: List<Txn>): Int {
        val current = load(ctx)
        val seen = current.map { it.id }.toHashSet()
        var added = 0
        for (t in incoming) if (seen.add(t.id)) { current.add(t); added++ }
        if (added > 0) {
            current.sortByDescending { it.epochMillis }
            save(ctx, current)
        }
        return added
    }
}
