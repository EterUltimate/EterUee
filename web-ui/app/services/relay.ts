import api from "~/services/api";
import type { HttpRelayRequest, HttpRelayResponse } from "~/types";

export function relayHttp(request: HttpRelayRequest): Promise<HttpRelayResponse> {
  return api.post<HttpRelayResponse>("relay/http", request);
}
