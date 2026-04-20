import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generateEncodedPath } from "../../utils/generateEncodedPath";
import type { AppRouteObject } from "../../routes";

export type IvaultSettingsTab = "geofence" | "timewindow";

export type IvaultSettingsParams = {
  realm: string;
  tab?: IvaultSettingsTab;
};

const IvaultSettingsSection = lazy(() =>
  import("../IvaultSettingsSection").then((m) => ({ default: m.default })),
);

export const IvaultSettingsRoute: AppRouteObject = {
  path: "/:realm/ivalt-settings",
  element: <IvaultSettingsSection />,
  breadcrumb: (t: any) => t("ivaltSettings"),
  handle: {
    access: "view-realm",
  },
};

export const IvaultSettingsRouteWithTab: AppRouteObject = {
  ...IvaultSettingsRoute,
  path: "/:realm/ivalt-settings/:tab",
};

export const toIvaultSettings = (
  params: IvaultSettingsParams,
): Partial<Path> => {
  const path = params.tab
    ? IvaultSettingsRouteWithTab.path
    : IvaultSettingsRoute.path;

  return {
    pathname: generateEncodedPath(path, params),
  };
};

export default [IvaultSettingsRoute, IvaultSettingsRouteWithTab];
