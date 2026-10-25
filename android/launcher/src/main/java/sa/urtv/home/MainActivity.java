    private void buildApps() {
        PackageManager pm = getPackageManager();
        artCache.evictAll();
        Map<String, App> byPkg = new HashMap<>();
        Map<String, String> given = AppNames.given(this);   // names the owner's agent set (dock.sh name)
        for (ResolveInfo ri : Dock.catalogue(this)) {
            ActivityInfo ai = ri.activityInfo;
            App a = new App();
            a.pkg = ai.packageName;
            a.label = AppNames.label(this, ri, given);
            a.component = new ComponentName(ai.packageName, ai.name);
            a.category = Dock.isTv(ri) ? Intent.CATEGORY_LEANBACK_LAUNCHER : Intent.CATEGORY_LAUNCHER;
            Drawable banner = artCache.get("b:" + a.pkg);
            if (banner == null) {
                banner = ai.loadBanner(pm);
                if (banner == null) banner = ai.applicationInfo.loadBanner(pm);
                if (banner != null) artCache.put("b:" + a.pkg, banner = fitted(banner, true));
            }
            a.isBanner = banner != null;
            if (banner != null) a.art = banner;
            else {
                a.art = artCache.get("i:" + a.pkg);
                if (a.art == null) artCache.put("i:" + a.pkg, a.art = fitted(ri.loadIcon(pm), false));
            }
            a.color = sampleColor(a.pkg, a.art);
            byPkg.put(a.pkg, a);
        }

        List<App> dock = new ArrayList<>();
        for (String p : dockApps) if (byPkg.containsKey(p)) dock.add(byPkg.remove(p));
        cardByPkg.clear();
        List<App> rest = new ArrayList<>(byPkg.values());
        Collections.sort(rest, (x, y) -> x.label.compareToIgnoreCase(y.label));
        // Fewer than MIN_DOCK of the chosen apps installed: top the dock up from the rest.
        while (dock.size() < MIN_DOCK && !rest.isEmpty()) dock.add(rest.remove(0));

        dockRow.removeAllViews();
        grid.removeAllViews();
        dockCards.clear();
        gridCards.clear();

        for (int i = 0; i < dock.size(); i++) {
            View tile = tile(dock.get(i), true);
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, -2, 1f);
            p.setMargins(i == 0 ? 0 : dp(10), 0, i == dock.size() - 1 ? 0 : dp(10), 0);
            dockRow.addView(tile, p);
        }

        LinearLayout row = null;
        for (int i = 0; i < rest.size(); i++) {
            if (i % cols == 0) {
                row = new LinearLayout(this);
                row.setClipChildren(false);
                row.setClipToPadding(false);
                grid.addView(row, llp(-1, -2, 0, 0, 0, dp(26)));
            }
            View tile = tile(rest.get(i), false);
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, -2, 1f);
            p.setMargins(i % cols == 0 ? 0 : dp(10), 0, i % cols == cols - 1 ? 0 : dp(10), 0);
            row.addView(tile, p);
        }
        // Pad the last row so tiles keep their width.
        if (row != null) for (int i = rest.size() % cols; i != 0 && i < cols; i++) {
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, 1, 1f);
            p.setMargins(dp(10), 0, i == cols - 1 ? 0 : dp(10), 0);
            row.addView(new View(this), p);
        }
        appsLabel.setVisibility(rest.isEmpty() ? View.GONE : View.VISIBLE);
        wireFocus();

        if (!dock.isEmpty()) { title.setText(dock.get(0).label); setGlow(dock.get(0).color); }
        View last = lastFocusedId == View.NO_ID ? null : findViewById(lastFocusedId);
        if (last != null) last.requestFocus();
        else if (!dockCards.isEmpty()) dockCards.get(0).requestFocus();
    }
