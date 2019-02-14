import Reflux from 'reflux';

import SystemActions from "../actions/SystemActions";
import RESTClient from "../util/RESTClient";

class SystemStore extends Reflux.Store {

    constructor() {
        super();
        this.listenables = SystemActions;
    }

    onGetStatus() {
        let self = this;

        RESTClient.get("/system/status", {}, function(response) {
            self.setState({systemStatus: response.data.status});
        });
    }

}

export default SystemStore;