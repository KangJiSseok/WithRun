export type ApiResponse<T = undefined> = {
  success: boolean;
  code: string;
  message: string;
  data: T;
  traceId: string;
};
