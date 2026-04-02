export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T | null;
  errors: unknown;
  timestamp: string;
}
