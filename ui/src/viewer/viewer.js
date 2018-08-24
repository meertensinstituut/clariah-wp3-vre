import React from "react";
import {withRouter} from "react-router-dom";
import PropTypes from 'prop-types';
import Switchboard from "../common/switchboard";

class Viewer extends React.Component {

    constructor(props) {
        super(props);
        this.state = {
            objectId: this.props.match.params.objectId,
            viewerFile: null
        };
        this.getViewOfObject();
    }

    getViewOfObject() {
        const params = {"params": [{"name": "input", "type": "file", "value": this.state.objectId}]};
        Switchboard.postDeployment("VIEWER", params).done((deployData) => {
            Switchboard.getDeploymentStatusWhenStatus(deployData.workDir, "FINISHED").done((viewerData) => {
                this.setState({
                    viewerFileContent: {__html: viewerData.viewerFileContent},
                    viewerFileName: viewerData.viewerFile
                });
            });
        });
    }

    render() {
        const viewerFile = this.state.viewerFileContent
            ? <div dangerouslySetInnerHTML={this.state.viewerFileContent}/>
            : <i className="fa fa-spinner fa-spin" aria-hidden="true" />;

        return (
            <div>
                <h1>Viewing file {this.state.objectId} using <code>{this.props.viewer}</code></h1>
                <div>{this.props.content}</div>
                {viewerFile}
            </div>
        );
    }
}

Viewer.propTypes = {
    match: PropTypes.shape({
        params: PropTypes.shape({
            objectId: PropTypes.string.isRequired
        })
    })
};

Viewer.defaultProps = {
    viewer: "VIEWER",
};

export default withRouter(Viewer);
