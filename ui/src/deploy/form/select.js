import React from "react";
import {ControlLabel, FormControl, FormGroup, HelpBlock} from 'react-bootstrap';

export default class Select extends React.Component {

    constructor(props) {
        super(props);
        this.state = {
            value: ""
        }
    }

    handleValidationFormBasicText() {
        const length = this.state.value.length;
        if (length > 10) return 'success';
        if (length > 5) return 'warning';
        if (length > 0) return 'error';
        return null;
    }

    render() {
        let multiple = this.props.param.maximumCardinality <= 1;
        return (
            <FormGroup controlId="formControlsSelect">
                <ControlLabel>{this.props.param.label}</ControlLabel>
                <FormControl.Feedback/>
                <HelpBlock>{this.props.param.description}</HelpBlock>
                <FormControl componentClass="select" placeholder="select" multiple={multiple}>
                    {this.props.param.values.map(function (value, i) {
                        return (
                            <option key={i} value={value.value}>
                                {value.label}
                                {(value.label!==undefined && value.description !== undefined) ? " - " : ""}
                                {value.description}
                            </option>
                        );
                    }, this)}
                </FormControl>
            </FormGroup>
        );
    }
}