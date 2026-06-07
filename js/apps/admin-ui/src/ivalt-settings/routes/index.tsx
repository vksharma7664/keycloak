import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generateEncodedPath } from "../../utils/generateEncodedPath";
import type { AppRouteObject } from "../../routes";

export type IvaltRealmParams = {
  realm: string;
};

const GeoFenceSection = lazy(() => import("../GeoFenceSection"));
const TimeWindowSection = lazy(() => import("../TimeWindowSection"));
const IvaltConfigSection = lazy(() => import("../IvaltConfigSection"));

export const GeoFencesRoute: AppRouteObject = {
  path: "/:realm/geofences",
  element: <GeoFenceSection />,
  breadcrumb: (t) => t("geoFences"),
  handle: {
    access: "view-realm",
  },
};

export const TimeWindowsRoute: AppRouteObject = {
  path: "/:realm/time-windows",
  element: <TimeWindowSection />,
  breadcrumb: (t) => t("timeWindows"),
  handle: {
    access: "view-realm",
  },
};

export const IvaltConfigRoute: AppRouteObject = {
  path: "/:realm/ivalt-settings",
  element: <IvaltConfigSection />,
  breadcrumb: (t) => t("ivaltSettings"),
  handle: {
    access: "view-realm",
  },
};

export const toGeoFences = (params: IvaltRealmParams): Partial<Path> => ({
  pathname: generateEncodedPath(GeoFencesRoute.path, params),
});

export const toTimeWindows = (params: IvaltRealmParams): Partial<Path> => ({
  pathname: generateEncodedPath(TimeWindowsRoute.path, params),
});

export const toIvaltConfig = (params: IvaltRealmParams): Partial<Path> => ({
  pathname: generateEncodedPath(IvaltConfigRoute.path, params),
});

export default [GeoFencesRoute, TimeWindowsRoute, IvaltConfigRoute];
