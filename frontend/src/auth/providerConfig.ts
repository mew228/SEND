export type OAuthProviderConfig = {
  id: string;
  label: string;
  enabled: boolean;
};

export const AUTH_PROVIDER_CONFIG = {
  emailPasswordEnabled: true,
  oauthProviders: [
    { id: "google", label: "Google", enabled: false },
    { id: "github", label: "GitHub", enabled: false },
  ] satisfies OAuthProviderConfig[],
};
