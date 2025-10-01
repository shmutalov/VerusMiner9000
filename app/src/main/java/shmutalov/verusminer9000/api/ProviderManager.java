// Copyright (c) 2020, Scala
//
// Please see the included LICENSE file for more information.

package shmutalov.verusminer9000.api;

import java.util.ArrayList;
import shmutalov.verusminer9000.Config;

public final class ProviderManager {

    private static final ArrayList<PoolItem> mPools = new ArrayList<>();

    public static void add(PoolItem poolItem) {
        mPools.add(poolItem);
    }

    public static void add(String key, String pool,String port, int poolType, String poolUrl, String poolIP) {
        mPools.add(new PoolItem(key, pool, port, poolType, poolUrl, poolIP));
    }

    public static void add(String key, String pool, String port, int poolType, String poolUrl, String poolIP, String poolApi) {
        mPools.add(new PoolItem(key, pool, port, poolType, poolUrl, poolIP, poolApi, "",""));
    }

    public static PoolItem[] getPools() {
        return mPools.toArray(new PoolItem[0]);
    }

    public static PoolItem getPoolById(int idx) {
        return mPools.get(idx);
    }

    public static PoolItem getPoolById(String idx) {
        int index = Integer.parseInt(idx);

        if (idx.equals("") || mPools.size() < index || mPools.size() == 0) {
            return null;
        }

        return mPools.get(index);
    }

    public static final ProviderData data = new ProviderData();

    public static PoolItem getSelectedPool() {
        if(request.mPoolItem != null) {
            return request.mPoolItem;
        }

        String sp = Config.read("selected_pool");
        if (sp.equals("")) {
            return null;
        }

        return getPoolById(sp);
    }

    public static void afterSave() {
        if(request.mPoolItem != null)  {
            return;
        }

        PoolItem pi = getSelectedPool();
        if(pi == null) {
            return;
        }

        //mPools.clear();
        request.mPoolItem = pi;
        data.isNew = true;
        request.start();
    }

    public static final ProviderRequest request = new ProviderRequest();

    public static void generate() {
        request.stop();
        request.mPoolItem = null;
        //mPools.clear();

        if(!mPools.isEmpty())
            return;

        // User Defined
        add("custom", "custom", "3333", 0, "", "");

        // Verus Official pool
        add(
                "Verus Project (Official Pool)",
                "stratum+tcp://pool.verus.io",
                "9998",
                1000,
                "https://pool.verus.io/",
                ""
        );

        // PaddyPool
        add(
                "PaddyPool",
                "stratum+tcp://stratum-eu.paddypool.net",
                "9998",
                1000,
                "https://paddypool.net/",
                ""
        );

        // An Interesting Hole
        add(
                "An Interesting Hole",
                "stratum+tcp://verus.aninterestinghole.xyz",
                "9998",
                1000,
                "https://aninterestinghole.xyz/",
                ""
        );

        // VerusFarm
        add(
                "VerusFarm",
                "stratum+tcp://verus.farm",
                "9999",
                1000,
                "https://verus.farm/",
                ""
        );

        // Luckpool (NA)
        add(
                "Luckpool (NA)",
                "stratum+tcp://na.luckpool.net",
                "3956",
                1000,
                "https://luckpool.net/verus",
                ""
        );

        // Luckpool (EU)
        add(
                "Luckpool (EU)",
                "stratum+tcp://eu.luckpool.net",
                "3956",
                1000,
                "https://luckpool.net/verus",
                ""
        );

        // Luckpool (AP)
        add(
                "Luckpool (AP)",
                "stratum+tcp://ap.luckpool.net",
                "3956",
                1000,
                "https://luckpool.net/verus",
                ""
        );
    }
}
