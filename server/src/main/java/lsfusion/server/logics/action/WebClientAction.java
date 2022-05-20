
    private final Object resource;
    private final String resourceName;
    private final boolean isFile;
    private final boolean syncType;

    public WebClientAction(String resourceName, int size, boolean isFile, boolean syncType) {
        super(LocalizedString.create("Web client"), SetFact.toOrderExclSet(size, i -> new PropertyInterface()));
        this.isFile = isFile;
        this.syncType = syncType;

        if(isFile) {
            Result<String> fullPath = new Result<>();
            RawFileData fileData = ResourceUtils.findResourceAsFileData(resourceName, false, true, fullPath, "web");
            fileData.getID(); // to calculate the cache
            resource = fileData;
            this.resourceName = fullPath.result;
        } else {
            resource = resourceName;
            this.resourceName = resourceName;
        }
    }

    @Override
    protected FlowResult aspectExecute(ExecutionContext<PropertyInterface> context) throws SQLException, SQLHandledException {
        ArrayList<byte[]> values = new ArrayList<>();
        ArrayList<byte[]> types = new ArrayList<>();

        try {
            for (PropertyInterface orderInterface : getOrderInterfaces()) {
                ObjectValue objectValue = context.getKeys().get(orderInterface);
                values.add(serializeObject(objectValue.getValue()));
                types.add(TypeSerializer.serializeType(objectValue.getType()));
            }
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }

        ClientWebAction clientWebAction = new ClientWebAction(resource, resourceName, values, types, isFile, syncType);
        if (syncType)
            context.requestUserInteraction(clientWebAction);
        else
            context.delayUserInteraction(clientWebAction);

        return FlowResult.FINISH;
    }

    @Override
    public boolean hasFlow(ChangeFlowType type) {
        return true;
    }
}